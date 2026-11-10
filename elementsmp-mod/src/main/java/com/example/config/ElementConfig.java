package com.example.config;

import com.example.ability.Ability;
import com.example.ability.AbilityRegistry;
import com.example.element.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inhalt der config.json.
 */
public class ElementConfig {
	/** Basis-Wahrscheinlichkeit pro Element (Schluessel = Element-Key). */
	public Map<String, Double> probabilities = new LinkedHashMap<>();
	/** 0 = reiner Zufall nach Wahrscheinlichkeit. Hoeher = bereits oft vergebene Elemente werden seltener gewuerfelt. */
	public double balanceStrength = 0.6;
	/** Gibt Spielern automatisch ihr Fokus-Item. */
	public boolean giveFocusItem = true;
	/** Einstellungen pro Faehigkeit (Schluessel = Faehigkeits-ID). */
	public Map<String, AbilitySettings> abilities = new LinkedHashMap<>();

	/** Ergaenzt fehlende Eintraege mit Standardwerten. */
	public void fillDefaults() {
		if (probabilities == null) {
			probabilities = new LinkedHashMap<>();
		}
		if (abilities == null) {
			abilities = new LinkedHashMap<>();
		}
		for (Element element : Element.values()) {
			probabilities.putIfAbsent(element.key(), 1.0);
		}
		for (Element element : Element.values()) {
			for (Ability ability : AbilityRegistry.get(element)) {
				abilities.putIfAbsent(ability.id(), ability.defaults().copy());
			}
		}
	}

	public AbilitySettings settingsFor(Ability ability) {
		AbilitySettings s = abilities.get(ability.id());
		return s != null ? s : ability.defaults();
	}

	public double probabilityOf(Element element) {
		Double v = probabilities.get(element.key());
		return v == null ? 1.0 : Math.max(0.0, v);
	}
}
