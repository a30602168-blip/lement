package com.example.command;

import com.example.Elementsmp;
import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.element.Element;
import com.example.element.ElementManager;
import com.example.gui.ElementGui;
import com.example.util.CooldownManager;
import com.example.util.Text;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public final class ElementCommand {
	private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
	private static final SuggestionProvider<CommandSourceStack> ELEMENT_SUGGESTIONS = (context, builder) ->
		SharedSuggestionProvider.suggest(Arrays.stream(Element.values()).map(Element::key), builder);

	private ElementCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
	}

	private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("element")
			.executes(ctx -> overview(ctx.getSource().getPlayerOrException()))
			.then(Commands.literal("gui")
				.executes(ctx -> {
					ElementGui.open(ctx.getSource().getPlayerOrException());
					return 1;
				}))
			.then(Commands.literal("focus")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (Elementsmp.store().get(player.getUUID()) == null) {
						ctx.getSource().sendFailure(Component.literal("Du hast noch kein Element."));
						return 0;
					}
					ElementManager.forceFocus(player);
					return 1;
				}))
			.then(Commands.literal("cast")
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, 3))
					.executes(ctx -> {
						ServerPlayer player = ctx.getSource().getPlayerOrException();
						ElementManager.cast(player, IntegerArgumentType.getInteger(ctx, "nummer") - 1);
						return 1;
					})))
			.then(Commands.literal("info").requires(ADMIN)
				.then(Commands.argument("spieler", EntityArgument.player())
					.executes(ElementCommand::info)))
			.then(Commands.literal("set").requires(ADMIN)
				.then(Commands.argument("spieler", EntityArgument.player())
					.then(Commands.argument("element", StringArgumentType.word())
						.suggests(ELEMENT_SUGGESTIONS)
						.executes(ElementCommand::set))))
			.then(Commands.literal("reset").requires(ADMIN)
				.then(Commands.argument("spieler", EntityArgument.player())
					.executes(ElementCommand::reset)))
			.then(Commands.literal("reload").requires(ADMIN)
				.executes(ElementCommand::reload)));
	}

	private static MutableComponent elementName(Element element) {
		return Text.chat(element.displayName(), element.color()).withStyle(ChatFormatting.BOLD);
	}

	/** /element: Chat-Uebersicht und GUI. */
	private static int overview(ServerPlayer player) {
		Element element = Elementsmp.store().get(player.getUUID());
		if (element == null) {
			player.sendSystemMessage(Text.chat("Du hast noch kein Element.", ChatFormatting.RED));
			return 0;
		}
		player.sendSystemMessage(Text.chat("Dein Element: ", ChatFormatting.GRAY).append(elementName(element)));
		List<Ability> abilities = AbilityRegistry.get(element);
		for (int i = 0; i < abilities.size(); i++) {
			Ability ability = abilities.get(i);
			int remaining = ElementManager.remainingTicks(player, ability);
			String status = remaining > 0 ? "noch " + CooldownManager.format(remaining) : "bereit";
			player.sendSystemMessage(Text.chat(" " + (i + 1) + ". " + ability.name(), element.color())
				.append(Text.chat(" - " + ability.description() + " (" + status + ")", ChatFormatting.GRAY)));
		}
		ElementGui.open(player);
		return 1;
	}

	private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
		Element element = Elementsmp.store().get(target.getUUID());
		MutableComponent name = Text.chat(target.getName().getString(), ChatFormatting.WHITE);
		if (element == null) {
			ctx.getSource().sendSuccess(() -> name.append(Text.chat(" hat noch kein Element.", ChatFormatting.GRAY)), false);
			return 0;
		}
		ctx.getSource().sendSuccess(() -> name
			.append(Text.chat(" hat das Element ", ChatFormatting.GRAY))
			.append(elementName(element)), false);
		return 1;
	}

	private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
		String input = StringArgumentType.getString(ctx, "element");
		Element element = Element.fromKey(input);
		if (element == null) {
			ctx.getSource().sendFailure(Component.literal("Unbekanntes Element: " + input
				+ " (wind, sand, lava, wasser, erde, void)"));
			return 0;
		}
		ElementManager.setElement(target, element);
		target.sendSystemMessage(Text.chat("Dein Element wurde zu ", ChatFormatting.GRAY)
			.append(elementName(element)).append(Text.chat(" geaendert.", ChatFormatting.GRAY)));
		ctx.getSource().sendSuccess(() -> Text.chat(target.getName().getString() + " hat jetzt das Element ", ChatFormatting.GRAY)
			.append(elementName(element)), true);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "spieler");
		Element rolled = ElementManager.reset(target);
		ctx.getSource().sendSuccess(() -> Text.chat(target.getName().getString()
			+ " wurde zurueckgesetzt und hat neu das Element ", ChatFormatting.GRAY).append(elementName(rolled)), true);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		boolean ok = Elementsmp.reloadConfig();
		if (ok) {
			ctx.getSource().sendSuccess(() -> Text.chat("Konfiguration neu geladen.", ChatFormatting.GREEN), true);
			return 1;
		}
		ctx.getSource().sendFailure(Component.literal("config.json ist fehlerhaft, die alten Werte bleiben aktiv. Details in der Server-Konsole."));
		return 0;
	}
}
