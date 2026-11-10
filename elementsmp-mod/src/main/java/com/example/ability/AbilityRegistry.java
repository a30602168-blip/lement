package com.example.ability;

import com.example.element.Element;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Zentrale Zuordnung Element -> Faehigkeiten. Neue Faehigkeiten werden hier registriert.
 */
public final class AbilityRegistry {
	private static final Map<Element, List<Ability>> BY_ELEMENT = new EnumMap<>(Element.class);

	static {
		BY_ELEMENT.put(Element.WIND, WindAbilities.create());
		BY_ELEMENT.put(Element.SAND, SandAbilities.create());
		BY_ELEMENT.put(Element.LAVA, LavaAbilities.create());
		BY_ELEMENT.put(Element.WATER, WaterAbilities.create());
		BY_ELEMENT.put(Element.EARTH, EarthAbilities.create());
		BY_ELEMENT.put(Element.VOID, VoidAbilities.create());
	}

	private AbilityRegistry() {
	}

	public static List<Ability> get(Element element) {
		return BY_ELEMENT.getOrDefault(element, Collections.emptyList());
	}
}
