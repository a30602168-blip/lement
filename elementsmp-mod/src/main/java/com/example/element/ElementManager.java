package com.example.element;

import com.example.Elementsmp;
import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.config.AbilitySettings;
import com.example.config.ElementConfig;
import com.example.util.CooldownManager;
import com.example.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Kernlogik: Elementvergabe, Fokus-Item, Faehigkeiten wirken.
 */
public final class ElementManager {
	private static final String FOCUS_TAG = "elementsmp_focus";
	private static final Map<UUID, Integer> SELECTED = new HashMap<>();

	private ElementManager() {
	}

	// ---------------------------------------------------------------- Vergabe

	/** Wuerfelt ein Element nach konfigurierten Wahrscheinlichkeiten und bereits vergebenen Elementen. */
	public static Element roll() {
		ElementConfig config = Elementsmp.config();
		Map<Element, Integer> counts = Elementsmp.store().counts();
		Element[] all = Element.values();
		double[] weights = new double[all.length];
		double total = 0;
		for (int i = 0; i < all.length; i++) {
			double base = config.probabilityOf(all[i]);
			int assigned = counts.getOrDefault(all[i], 0);
			weights[i] = base / (1.0 + Math.max(0.0, config.balanceStrength) * assigned);
			total += weights[i];
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (total <= 0) {
			return all[random.nextInt(all.length)];
		}
		double r = random.nextDouble() * total;
		for (int i = 0; i < all.length; i++) {
			r -= weights[i];
			if (r < 0) {
				return all[i];
			}
		}
		return all[all.length - 1];
	}

	/** Weist einem Spieler beim ersten Join ein Element zu. Bestehende Zuweisungen bleiben unveraendert. */
	public static synchronized Element assignIfAbsent(ServerPlayer player) {
		Element existing = Elementsmp.store().get(player.getUUID());
		if (existing != null) {
			return existing;
		}
		Element rolled = roll();
		Elementsmp.store().set(player.getUUID(), rolled);
		player.sendSystemMessage(Text.chat("Dir wurde das Element ", ChatFormatting.GRAY)
			.append(Text.chat(rolled.displayName(), rolled.color()).withStyle(ChatFormatting.BOLD))
			.append(Text.chat(" zugeteilt! Nutze /element oder dein Fokus-Item.", ChatFormatting.GRAY)));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
		return rolled;
	}

	/** Setzt ein Element fest und aktualisiert das Fokus-Item. */
	public static void setElement(ServerPlayer player, Element element) {
		Elementsmp.store().set(player.getUUID(), element);
		SELECTED.remove(player.getUUID());
		Elementsmp.cooldowns().clear(player.getUUID());
		replaceFocus(player);
	}

	/** Entfernt die Zuweisung und wuerfelt sofort neu (Spieler ist online). */
	public static Element reset(ServerPlayer player) {
		Elementsmp.store().remove(player.getUUID());
		SELECTED.remove(player.getUUID());
		Elementsmp.cooldowns().clear(player.getUUID());
		Element rolled = assignIfAbsent(player);
		replaceFocus(player);
		return rolled;
	}

	public static void forget(UUID uuid) {
		SELECTED.remove(uuid);
		Elementsmp.cooldowns().clear(uuid);
	}

	// ---------------------------------------------------------------- Fokus-Item

	public static boolean isFocus(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.contains(FOCUS_TAG);
	}

	private static ItemStack createFocus(Element element) {
		List<Component> lore = new ArrayList<>();
		lore.add(Text.plain(element.description(), ChatFormatting.GRAY));
		lore.add(Component.empty());
		lore.add(Text.plain("Rechtsklick: Faehigkeit wirken", ChatFormatting.YELLOW));
		lore.add(Text.plain("Shift + Rechtsklick: Faehigkeit wechseln", ChatFormatting.YELLOW));
		ItemStack stack = Text.item(element.icon(),
			Text.bold("Fokus: " + element.displayName(), element.color()), lore, true);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(FOCUS_TAG, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	private static boolean hasFocus(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isFocus(inv.getItem(i))) {
				return true;
			}
		}
		return false;
	}

	private static void removeFocus(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (isFocus(inv.getItem(i))) {
				inv.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	private static void giveFocus(ServerPlayer player, Element element) {
		ItemStack stack = createFocus(element);
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	/** Gibt das Fokus-Item, falls der Spieler keines hat. */
	public static void ensureFocus(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null || !Elementsmp.config().giveFocusItem || hasFocus(player)) {
			return;
		}
		giveFocus(player, element);
	}

	/** Ersetzt vorhandene Fokus-Items durch eines des aktuellen Elements. */
	public static void replaceFocus(ServerPlayer player) {
		removeFocus(player);
		Element element = Elementsmp.store().get(player.getUUID());
		if (element != null && Elementsmp.config().giveFocusItem) {
			giveFocus(player, element);
		}
	}

	/** Gibt immer ein (weiteres) Fokus-Item, z. B. ueber /element focus. */
	public static void forceFocus(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element != null) {
			removeFocus(player);
			giveFocus(player, element);
		}
	}

	// ---------------------------------------------------------------- Faehigkeiten

	public static int selectedIndex(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			return 0;
		}
		int size = AbilityRegistry.get(element).size();
		int index = SELECTED.getOrDefault(player.getUUID(), 0);
		return size == 0 ? 0 : Math.floorMod(index, size);
	}

	public static void cycle(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			return;
		}
		List<Ability> abilities = AbilityRegistry.get(element);
		if (abilities.isEmpty()) {
			return;
		}
		int next = (selectedIndex(player) + 1) % abilities.size();
		SELECTED.put(player.getUUID(), next);
		Ability ability = abilities.get(next);
		player.displayClientMessage(Text.plain("Ausgewaehlt: ", ChatFormatting.GRAY)
			.append(Text.bold(ability.name(), element.color())), true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
			SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
	}

	public static AbilitySettings settings(Ability ability) {
		return Elementsmp.config().settingsFor(ability);
	}

	public static int remainingTicks(ServerPlayer player, Ability ability) {
		return Elementsmp.cooldowns().remainingTicks(player.getUUID(), ability.id(), Elementsmp.server().getTickCount());
	}

	/** Wirkt die Faehigkeit mit dem gegebenen Index (0-basiert). */
	public static void cast(ServerPlayer player, int index) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			player.sendSystemMessage(Text.chat("Du hast noch kein Element.", ChatFormatting.RED));
			return;
		}
		List<Ability> abilities = AbilityRegistry.get(element);
		if (index < 0 || index >= abilities.size()) {
			return;
		}
		Ability ability = abilities.get(index);
		int now = Elementsmp.server().getTickCount();
		int remaining = Elementsmp.cooldowns().remainingTicks(player.getUUID(), ability.id(), now);
		if (remaining > 0) {
			player.displayClientMessage(Text.plain(ability.name() + ": noch " + CooldownManager.format(remaining), ChatFormatting.RED), true);
			return;
		}
		AbilitySettings settings = settings(ability);
		boolean success;
		try {
			success = ability.action().cast(player, (ServerLevel) player.level(), settings);
		} catch (RuntimeException ex) {
			Elementsmp.LOGGER.error("Fehler bei Faehigkeit {}", ability.id(), ex);
			success = false;
		}
		if (success) {
			Elementsmp.cooldowns().start(player.getUUID(), ability.id(), now, settings.cooldownSeconds);
			player.displayClientMessage(Text.bold(ability.name(), element.color()), true);
		} else {
			player.displayClientMessage(Text.plain(ability.name() + " konnte nicht gewirkt werden.", ChatFormatting.RED), true);
		}
	}

	public static void castSelected(ServerPlayer player) {
		cast(player, selectedIndex(player));
	}
}
