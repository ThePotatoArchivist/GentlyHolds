package archives.tater.gentlyholds;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalEntityTypeTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public class GentlyHoldsConfig extends MidnightConfig {
    @Entry
    public static EntityRestriction entityRestriction = EntityRestriction.ANIMAL;
    @Entry(min = 0)
    public static float maxWidth = 2;
    @Entry(min = 0)
    public static float maxHeight = 2;
    @Entry
    public static boolean emptyHands = true;
    @Entry
    public static boolean spawnDrop = true;
    @Entry
    public static boolean canWearHat = true;

    @SuppressWarnings("unused")
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
        NOT_AGGROED { public boolean canPickup(PlayerEntity player, Entity target) {
            return target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof MobEntity mobEntity) || getTarget(mobEntity) == null);
        } },
        NOT_AGGROED_PLAYER { public boolean canPickup(PlayerEntity player, Entity target) {
            return target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof MobEntity mobEntity) || getTarget(mobEntity) != player);
        } },
        ANIMAL { public boolean canPickup(PlayerEntity player, Entity target) {
            return target instanceof AnimalEntity || target instanceof WaterCreatureEntity || target instanceof AmbientEntity;
        } },
        OWNED { public boolean canPickup(PlayerEntity player, Entity target) {
            return target instanceof Tameable tameable && tameable.getOwner() == player;
        } };

        public abstract boolean canPickup(PlayerEntity player, Entity target);

        private static @Nullable LivingEntity getTarget(MobEntity mobEntity) {
            var target = mobEntity.getTarget();
            if (target != null)
                return target.isAlive() ? target : null;
            var targetOptional = mobEntity.getBrain().getOptionalMemory(MemoryModuleType.ATTACK_TARGET);
            //noinspection OptionalAssignedToNull
            if (targetOptional == null || targetOptional.isEmpty()) return null;
            var targetActual = targetOptional.get();
            return targetActual.isAlive() ? targetActual : null;
        }
    }
}
