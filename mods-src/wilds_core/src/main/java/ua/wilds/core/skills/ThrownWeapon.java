package ua.wilds.core.skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ua.wilds.core.WildsCore;

/**
 * A melee weapon thrown with the warrior's "Метальник" ability: flies like a thrown trident, deals the given damage
 * to the first creature it hits and comes back to the thrower's inventory (or drops there, if it is full).
 */
public class ThrownWeapon extends ThrowableItemProjectile {
    private float damage = 4;

    public ThrownWeapon(EntityType<? extends ThrownWeapon> type, Level level) {
        super(type, level);
    }

    public static ThrownWeapon of(ServerPlayer thrower, ItemStack weapon, float damage) {
        ThrownWeapon w = new ThrownWeapon(WildsCore.THROWN_WEAPON.get(), thrower.level());
        w.setOwner(thrower);
        w.setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
        w.setItem(weapon);
        w.damage = damage;
        return w;
    }

    protected Item getDefaultItem() {
        return Items.IRON_SWORD;
    }

    protected double getDefaultGravity() {
        return 0.04;
    }

    protected void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        if (target == getOwner() || !(target instanceof LivingEntity)) return;
        Entity owner = getOwner();
        target.hurt(damageSources().thrown(this, owner != null ? owner : this), damage);
        level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1f, 1.1f);
    }

    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.ENTITY && ((EntityHitResult) result).getEntity() == getOwner()) return;
        if (!level().isClientSide) giveBack();
    }

    public void tick() {
        super.tick();
        // never lost: an old flight (out of the world, stuck somewhere) brings the weapon back after 10 seconds
        if (!level().isClientSide && (tickCount > 200 || getY() < level().getMinBuildHeight() - 16)) giveBack();
    }

    private void giveBack() {
        ItemStack weapon = getItem().copy();
        if (getOwner() instanceof Player p && p.isAlive() && !weapon.isEmpty()) {
            if (p instanceof ServerPlayer sp) weapon.hurtAndBreak(2, (ServerLevel) level(), sp, i -> {});
            if (!weapon.isEmpty() && !p.getInventory().add(weapon)) p.drop(weapon, false);
        } else if (!weapon.isEmpty()) {
            level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), weapon));
        }
        setItem(ItemStack.EMPTY);
        discard();
    }
}
