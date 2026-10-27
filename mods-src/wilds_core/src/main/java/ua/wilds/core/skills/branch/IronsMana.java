package ua.wilds.core.skills.branch;

import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import ua.wilds.core.skills.SkillState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** The mage abilities that work with Iron's Spells' mana; loaded only when that mod is installed. */
final class IronsMana {
    private static final long HOUR = 72000;
    /** players whose next mana spend is free (set when a spell starts, taken when its mana is spent) */
    private static final Set<UUID> pending = new HashSet<>();

    private IronsMana() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((SpellPreCastEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && SkillState.has(p, "free_spell") && SkillState.ready(p, "free_spell")) {
                pending.add(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((ChangeManaEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || e.getNewMana() >= e.getOldMana() || !pending.remove(p.getUUID())) return;
            if (!SkillState.use(p, "free_spell", HOUR)) return;
            e.setNewMana(e.getOldMana());
            p.displayClientMessage(Component.translatable("wilds_core.skill.free_spell.used").withStyle(ChatFormatting.AQUA), true);
        });
    }

    private static float max(ServerPlayer p) {
        var inst = p.getAttribute(AttributeRegistry.MAX_MANA);
        return inst == null ? 100f : (float) inst.getValue();
    }

    static void restoreFraction(ServerPlayer p, float fraction) {
        MagicData d = MagicData.getPlayerMagicData(p);
        d.setMana(Math.min(max(p), d.getMana() + max(p) * fraction));
    }

    /** spends mana if there is that much */
    static boolean pay(ServerPlayer p, float mana) {
        MagicData d = MagicData.getPlayerMagicData(p);
        if (d.getMana() < mana) return false;
        d.setMana(d.getMana() - mana);
        return true;
    }
}
