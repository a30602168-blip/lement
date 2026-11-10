package com.example.element;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Die sechs Elemente des Servers.
 */
public enum Element {
	WIND("wind", "Wind", ChatFormatting.AQUA, Items.FEATHER,
		"Schnell und beweglich. Kontrolliert den Raum, macht aber kaum Schaden."),
	SAND("sand", "Sand", ChatFormatting.YELLOW, Items.GLOWSTONE_DUST,
		"Der Kontrolleur. Blendet, bremst und blockt, hat aber kaum Schadenspotenzial."),
	LAVA("lava", "Lava", ChatFormatting.GOLD, Items.BLAZE_ROD,
		"Hoher Schaden mit Feuer, dafuer kaum Mobilitaet und wenig Heilung."),
	WATER("wasser", "Wasser", ChatFormatting.BLUE, Items.HEART_OF_THE_SEA,
		"Der Unterstuetzer. Heilt und stoesst zurueck, macht aber wenig Schaden."),
	EARTH("erde", "Erde", ChatFormatting.GREEN, Items.FLINT,
		"Der Tank. Waende und Ruestung, dafuer langsam und ohne Fluchtmoeglichkeit."),
	VOID("void", "Void", ChatFormatting.DARK_PURPLE, Items.ECHO_SHARD,
		"Teleport und dunkle Energie. Maechtig, aber die Kraft kostet Lebensenergie.");

	private final String key;
	private final String displayName;
	private final ChatFormatting color;
	private final Item icon;
	private final String description;

	Element(String key, String displayName, ChatFormatting color, Item icon, String description) {
		this.key = key;
		this.displayName = displayName;
		this.color = color;
		this.icon = icon;
		this.description = description;
	}

	public String key() {
		return key;
	}

	public String displayName() {
		return displayName;
	}

	public ChatFormatting color() {
		return color;
	}

	public Item icon() {
		return icon;
	}

	public String description() {
		return description;
	}

	/** Findet ein Element ueber seinen Schluessel (deutsch oder englisch). */
	public static Element fromKey(String input) {
		if (input == null) {
			return null;
		}
		String lower = input.toLowerCase(Locale.ROOT);
		for (Element element : values()) {
			if (element.key.equals(lower) || element.name().toLowerCase(Locale.ROOT).equals(lower)) {
				return element;
			}
		}
		return null;
	}
}
