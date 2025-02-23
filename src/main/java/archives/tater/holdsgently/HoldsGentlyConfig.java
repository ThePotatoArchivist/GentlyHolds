package archives.tater.holdsgently;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalEntityTypeTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

@SuppressWarnings("unused")
public class HoldsGentlyConfig extends MidnightConfig {
    @Entry
    public static EntityRestriction entityRestriction = EntityRestriction.ANIMAL;
    @Entry
    public static boolean emptyHands = true;
    @Entry
    public static boolean canWearHat = true;

    public enum EntityRestriction {
        ANY { public boolean canPickup(PlayerEntity player, Entity target) {
            return true;
        } },
        LIVING { public boolean canPickup(PlayerEntity player, Entity target) {
            return target.isLiving();
        } },
        NON_BOSS { public boolean canPickup(PlayerEntity player, Entity target) {
            return target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES);
        } },
        NON_MONSTER { public boolean canPickup(PlayerEntity player, Entity target) {
            return target.isLiving() && !(target instanceof Monster);
        } },
        ANIMAL { public boolean canPickup(PlayerEntity player, Entity target) {
            return target instanceof AnimalEntity;
        } },
        OWNED { public boolean canPickup(PlayerEntity player, Entity target) {
                return target instanceof Tameable tameable && tameable.getOwner() == player;
        } };

        public abstract boolean canPickup(PlayerEntity player, Entity target);
    }
}
