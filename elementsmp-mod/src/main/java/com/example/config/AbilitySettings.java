package com.example.config;

/**
 * Konfigurierbare Werte einer Faehigkeit. Welche Werte genutzt werden, haengt von der Faehigkeit ab.
 */
public class AbilitySettings {
	public double cooldownSeconds = 10;
	public double power = 1;
	public double radius = 5;
	public double durationSeconds = 5;
	public double damage = 0;
	public double selfDamage = 0;

	public static AbilitySettings of(double cooldown, double power, double radius, double duration, double damage, double selfDamage) {
		AbilitySettings s = new AbilitySettings();
		s.cooldownSeconds = cooldown;
		s.power = power;
		s.radius = radius;
		s.durationSeconds = duration;
		s.damage = damage;
		s.selfDamage = selfDamage;
		return s;
	}

	public AbilitySettings copy() {
		return of(cooldownSeconds, power, radius, durationSeconds, damage, selfDamage);
	}
}
