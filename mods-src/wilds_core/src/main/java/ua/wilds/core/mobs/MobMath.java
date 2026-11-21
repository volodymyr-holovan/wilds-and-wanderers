package ua.wilds.core.mobs;

/**
 * The balance formulas of pack/tools/gen_mob_balance.py, version 2 (docs/balance/mob-levels.md shows their results).
 * Rates and tables come from data/wilds_core/wilds_mobs/variants.json, so the document and the game cannot drift apart.
 */
public final class MobMath {
    private MobMath() {}

    /** one full-strength hit of a player at power level L */
    public static double gearDamage(double l) {
        return 6.0 * Math.pow((l + 15.0) / 30.0, 0.8);
    }

    /** the power level a weapon hit of this size is worth (inverse of gearDamage) */
    public static double weaponLevel(double dmg) {
        return Math.max(0, 30.0 * Math.pow(Math.max(dmg, 0.1) / 6.0, 1.25) - 15.0);
    }

    /** reference player's {armor, toughness, protection EPF, max health} at power level L (piecewise linear) */
    public static double[] defence(double l) {
        double[][] t = MobData.defence;
        l = Math.max(t[0][0], Math.min(t[t.length - 1][0], l));
        for (int i = 0; i + 1 < t.length; i++) {
            if (l >= t[i][0] && l <= t[i + 1][0]) {
                double f = (l - t[i][0]) / (t[i + 1][0] - t[i][0]);
                double[] r = new double[4];
                for (int j = 0; j < 4; j++) r[j] = t[i][j + 1] + (t[i + 1][j + 1] - t[i][j + 1]) * f;
                return r;
            }
        }
        double[] last = t[t.length - 1];
        return new double[]{last[1], last[2], last[3], last[4]};
    }

    /** vanilla 1.21 CombatRules.getDamageAfterAbsorb */
    public static double afterArmor(double dmg, double armor, double tough) {
        double f = 2.0 + tough / 4.0;
        double eff = Math.max(Math.min(armor - dmg / f, 20.0), armor * 0.2);
        return dmg * (1.0 - eff / 25.0);
    }

    /** health measured in raw 10-damage hits */
    public static double ehp(double armor, double tough, double epf, double hp) {
        return hp / (afterArmor(10.0, armor, tough) * (1.0 - Math.min(20.0, epf) / 25.0));
    }

    /** the power level a defence is worth: the lowest reference level whose defence is at least as good */
    public static double defenceLevel(double armor, double tough, double epf, double hp) {
        double e = ehp(armor, tough, epf, hp);
        for (int l = 0; l <= 400; l++) {
            double[] d = defence(l);
            if (ehp(d[0], d[1], d[2], d[3]) >= e) return l;
        }
        return 400;
    }

    /** the weapon counts more: mob health is measured in the player's hits, and a strong armor alone should not make every mob a sponge */
    public static double powerLevel(double dmg, double armor, double tough, double epf, double hp) {
        return Math.min(400, MobData.weaponWeight * weaponLevel(dmg) + (1 - MobData.weaponWeight) * defenceLevel(armor, tough, epf, hp));
    }

    public static double hpMult(double l) { return 1.0 + MobData.hpRate * l; }
    public static double dmgMult(double l) { return 1.0 + MobData.dmgRate * l; }
    public static double armorAdd(double l, int tier) { return Math.min(10.0, l / 35.0) + 0.4 * tier; }
    public static double toughAdd(double l) { return Math.min(6.0, l / 60.0); }
    public static double speedAdd(double l) { return Math.min(0.15, l / 2500.0); }
    public static double kbAdd(double l) { return Math.min(0.40, l / 1000.0); }
    public static double resist(double l) { return Math.min(0.25, l / 1500.0); }

    /** max health of a tier-k creature at mob level l, wearing this much armor (its own and ours): the same hits whatever its armor */
    public static double tierHp(int k, double l, double armor, double tough) {
        double u = MobData.unlock(k);
        return MobData.tierHits * afterArmor(gearDamage(u), armor, tough) * hpMult(l) / hpMult(u);
    }

    /** a tier-k creature's damage multiplier at mob level l (on its own damage) */
    public static double tierDmgMult(int k, double l) {
        return dmgMult(l) * (1 + MobData.kindDmgStep * k);
    }

    public static double bossHp(double nativeHp, double l) {
        return Math.max(nativeHp * (1 + MobData.bossHpRate * l), MobData.bossHits * gearDamage(l));
    }

    public static double bossDmgMult(double l) {
        return 1 + MobData.bossDmgRate * l;
    }
}
