package ua.wilds.core.mobs;

/**
 * The balance formulas of pack/tools/gen_mob_balance.py (docs/balance/mob-levels.md shows their results). Every number
 * comes from the settings of data/wilds_core/wilds_mobs/variants.json, so the document and the game cannot drift apart.
 */
public final class MobMath {
    private MobMath() {}

    /** damage of one full-strength hit of the reference player at level L */
    public static double gearDamage(double l) {
        return 6.0 * Math.pow((l + 15.0) / 30.0, 0.8);
    }

    /** reference player's {armor, toughness, protection EPF, max health} at level L (piecewise linear) */
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

    /** damage the reference player of level L takes from a raw hit */
    public static double toPlayer(double raw, double l) {
        double[] d = defence(l);
        return afterArmor(raw, d[0], d[1]) * (1.0 - Math.min(20.0, d[2]) / 25.0);
    }

    /** raw damage that kills the reference player of level L in that many hits */
    public static double rawForHits(double l, double hits) {
        double hp = defence(l)[3], lo = 0, hi = 2000;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (toPlayer(mid, l) * hits >= hp) hi = mid; else lo = mid;
        }
        return hi;
    }

    public static double hpMult(double l) { return 1.0 + MobData.hpRate * l; }
    public static double dmgMult(double l) { return 1.0 + MobData.dmgRate * l; }
    public static double armorAdd(double l, int tier) { return Math.min(12.0, l / 30.0) + 0.6 * tier; }
    public static double toughAdd(double l) { return Math.min(8.0, l / 50.0); }
    public static double speedAdd(double l) { return Math.min(0.20, l / 2000.0); }
    public static double kbAdd(double l) { return Math.min(0.40, l / 1000.0); }
    public static double resist(double l) { return Math.min(0.30, l / 1300.0); }

    /** max health of a tier-k mob at mob level l */
    public static double tierHp(int k, double l) {
        double u = MobData.unlock(k);
        double base = MobData.tierHits * afterArmor(gearDamage(u), armorAdd(u, k), toughAdd(u));
        return base * hpMult(l) / hpMult(u);
    }

    /** raw damage of a tier-k mob at mob level l */
    public static double tierDmg(int k, double l) {
        double u = MobData.unlock(k);
        return rawForHits(u, MobData.tierPlayerHits) * dmgMult(l) / dmgMult(u);
    }

    public static double bossHp(double nativeHp, double l) {
        return Math.max(nativeHp * (1 + MobData.bossHpRate * l), MobData.bossHits * gearDamage(l));
    }

    public static double bossDmg(double nativeDmg, double l) {
        return Math.max(nativeDmg * (1 + MobData.bossDmgRate * l), rawForHits(l, MobData.bossPlayerHits));
    }
}
