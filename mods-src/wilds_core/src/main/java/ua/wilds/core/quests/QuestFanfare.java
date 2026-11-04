package ua.wilds.core.quests;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * A finished quest is felt, not only read in a toast: a chime and a short swirl of golden sparks around the player; a
 * finished chapter gets the "challenge complete" fanfare and a burst of fireworks sparks. Only the player(s) the quest
 * was finished for hear and see it (with personal quests, the player alone).
 */
public final class QuestFanfare {
    private QuestFanfare() {}

    public static void register() {
        ObjectCompletedEvent.QUEST.register(e -> {
            for (ServerPlayer p : e.getNotifiedPlayers()) quest(p);
            return EventResult.pass();
        });
        ObjectCompletedEvent.CHAPTER.register(e -> {
            for (ServerPlayer p : e.getNotifiedPlayers()) chapter(p);
            return EventResult.pass();
        });
    }

    private static void quest(ServerPlayer p) {
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8, r = 0.9;
            p.serverLevel().sendParticles(ParticleTypes.WAX_ON, p.getX() + Math.cos(a) * r, p.getY() + 0.3 + i * 0.1, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 10, 0.5, 0.6, 0.5, 0);
    }

    private static void chapter(ServerPlayer p) {
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1f);
        p.serverLevel().sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1.5, p.getZ(), 60, 0.3, 0.6, 0.3, 0.15);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
    }
}
