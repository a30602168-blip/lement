package com.example.ability;

import com.example.config.AbilitySettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface AbilityAction {
	/**
	 * Fuehrt die Faehigkeit aus.
	 *
	 * @return true, wenn die Faehigkeit ausgeloest wurde (Cooldown startet), false bei Fehlschlag
	 */
	boolean cast(ServerPlayer player, ServerLevel level, AbilitySettings settings);
}
