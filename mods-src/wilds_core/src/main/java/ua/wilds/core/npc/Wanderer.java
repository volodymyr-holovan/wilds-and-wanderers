package ua.wilds.core.npc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A wandering quest giver ("Мандрівник з дорученням"): a person with a story, standing somewhere in a structure.
 * Mortal (a player or an accident can kill them, and their quest fails then), never despawns, keeps near the spot it
 * was put in, and starts talking by itself when a player comes close. The persona and its jobs come from
 * {@link NpcQuest}; the conversation is {@link Dialogue}. Which sign hangs above it («!», «?» or none) depends on the
 * player looking, so the server sends every player their own signs (NpcWorld.syncMarks).
 */
public class Wanderer extends PathfinderMob {
    /** the persona's key (see {@link NpcQuest#persona}) */
    private static final EntityDataAccessor<String> QUEST = SynchedEntityData.defineId(Wanderer.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(Wanderer.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(Wanderer.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TALKING = SynchedEntityData.defineId(Wanderer.class, EntityDataSerializers.BOOLEAN);

    /** on the client: the signs this player sees (entity id -> Dialogue.MARK_*), from the server */
    public static volatile Map<Integer, Integer> clientMarks = Map.of();

    private BlockPos home;
    /** when each player may be greeted again (game time) */
    private final Map<UUID, Long> greeted = new HashMap<>();

    public Wanderer(EntityType<? extends Wanderer> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 16);
    }

    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(QUEST, "");
        b.define(SKIN, "minecraft:textures/entity/player/wide/steve.png");
        b.define(SLIM, false);
        b.define(TALKING, false);
    }

    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        goalSelector.addGoal(2, new StayHome());
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10f, 1f));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5, 0.002f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** puts the persona and the quest on a freshly spawned NPC */
    public void setup(NpcQuest q, BlockPos home) {
        entityData.set(QUEST, q.persona);
        entityData.set(SKIN, q.skin);
        entityData.set(SLIM, q.slim);
        this.home = home;
        setCustomName(Component.literal(q.name + (q.title.isEmpty() ? "" : ", " + q.title)));
        ResourceLocation item = ResourceLocation.tryParse(q.item);
        if (item != null) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BuiltInRegistries.ITEM.get(item)));
        setDropChance(EquipmentSlot.MAINHAND, 0);
    }

    public String questKey() {
        return entityData.get(QUEST);
    }

    /** the giver's own quest (name, look, place); the job a player is at comes from {@link Dialogue#current} */
    public NpcQuest persona() {
        return NpcQuest.get(questKey());
    }

    public ResourceLocation skin() {
        ResourceLocation rl = ResourceLocation.tryParse(entityData.get(SKIN));
        return rl != null ? rl : ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    public boolean slim() {
        return entityData.get(SLIM);
    }

    /** on the client: the sign above this giver for the player here */
    public int mark() {
        return clientMarks.getOrDefault(getId(), 0);
    }

    public boolean talking() {
        return entityData.get(TALKING);
    }

    void setTalking(boolean t) {
        entityData.set(TALKING, t);
    }

    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 10 != 0) return;
        if (tickCount % 100 == 0) {
            if (getHealth() < getMaxHealth()) heal(1);
            NpcWorld.moved(this);
        }
        if (talking() || persona() == null) return;
        // a player who comes close is greeted (and the conversation opens) once in a while
        long now = level().getGameTime();
        for (Player p : level().players()) {
            if (!(p instanceof ServerPlayer sp) || sp.isSpectator() || distanceToSqr(p) > 25 || !hasLineOfSight(p)) continue;
            if (greeted.getOrDefault(p.getUUID(), 0L) > now || Dialogue.busy(sp)) continue;
            if (!Dialogue.wantsToTalk(sp, this)) continue;
            greeted.put(p.getUUID(), now + 1200);
            Dialogue.open(sp, this);
            break;
        }
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp && persona() != null && !Dialogue.busy(sp)) Dialogue.open(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide) NpcWorld.npcDied(this);
    }

    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("WildsQuest", questKey());
        tag.putString("WildsSkin", entityData.get(SKIN));
        tag.putBoolean("WildsSlim", slim());
        if (home != null) tag.putLong("WildsHome", home.asLong());
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(QUEST, tag.getString("WildsQuest"));
        if (tag.contains("WildsSkin")) entityData.set(SKIN, tag.getString("WildsSkin"));
        entityData.set(SLIM, tag.getBoolean("WildsSlim"));
        if (tag.contains("WildsHome")) home = BlockPos.of(tag.getLong("WildsHome"));
    }

    /** walks back when it strayed more than 6 blocks from its spot */
    private final class StayHome extends Goal {
        StayHome() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        public boolean canUse() {
            return home != null && !talking() && distanceToSqr(Vec3.atBottomCenterOf(home)) > 36;
        }

        public void start() {
            getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.7);
        }

        public boolean canContinueToUse() {
            return !getNavigation().isDone();
        }
    }

    public void customServerAiStep() {
        super.customServerAiStep();
        if (talking()) getNavigation().stop();
    }
}
