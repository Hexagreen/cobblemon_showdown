package com.newbulaco.showdown.client.tooltip;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;

/**
 * formula: type = floor((a + 2b + 4c + 8d + 16e + 32f) * 15 / 63)
 * where a-f are the least significant bits of HP, Atk, Def, Speed, SpA, SpD IVs
 */
public class HiddenPowerCalculator {

    private static final String[] TYPES = {
        "Fighting", "Flying", "Poison", "Ground", "Rock", "Bug",
        "Ghost", "Steel", "Fire", "Water", "Grass", "Electric",
        "Psychic", "Ice", "Dragon", "Dark"
    };

    public static String calculateHiddenPowerType(Pokemon pokemon) {
        int hpIV = pokemon.getIvs().getOrDefault(Stats.HP);
        int atkIV = pokemon.getIvs().getOrDefault(Stats.ATTACK);
        int defIV = pokemon.getIvs().getOrDefault(Stats.DEFENCE);
        int spdIV = pokemon.getIvs().getOrDefault(Stats.SPEED);
        int spAtkIV = pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK);
        int spDefIV = pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE);

        int a = hpIV % 2;
        int b = atkIV % 2;
        int c = defIV % 2;
        int d = spdIV % 2;
        int e = spAtkIV % 2;
        int f = spDefIV % 2;

        int typeIndex = (int) Math.floor((a + 2*b + 4*c + 8*d + 16*e + 32*f) * 15.0 / 63.0);
        return TYPES[typeIndex];
    }

    /**
     * gen 6+ Hidden Power is always 60 power, but this shows the theoretical gen 2-5 power (30-70).
     */
    public static int calculateHiddenPowerPower(Pokemon pokemon) {
        int hpIV = pokemon.getIvs().getOrDefault(Stats.HP);
        int atkIV = pokemon.getIvs().getOrDefault(Stats.ATTACK);
        int defIV = pokemon.getIvs().getOrDefault(Stats.DEFENCE);
        int spdIV = pokemon.getIvs().getOrDefault(Stats.SPEED);
        int spAtkIV = pokemon.getIvs().getOrDefault(Stats.SPECIAL_ATTACK);
        int spDefIV = pokemon.getIvs().getOrDefault(Stats.SPECIAL_DEFENCE);

        int u = (hpIV / 2) % 2;
        int v = (atkIV / 2) % 2;
        int w = (defIV / 2) % 2;
        int x = (spdIV / 2) % 2;
        int y = (spAtkIV / 2) % 2;
        int z = (spDefIV / 2) % 2;

        return (int) Math.floor((u + 2*v + 4*w + 8*x + 16*y + 32*z) * 40.0 / 63.0) + 30;
    }
}
