package archives.tater.gentlyholds;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import eu.midnightdust.lib.config.MidnightConfig;
import org.jetbrains.annotations.Nullable;

public class GentlyHoldsConfig extends MidnightConfig {
    @Entry
    public static EntityRestriction entityRestriction = EntityRestriction.NON_MONSTER;
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
    @Entry
    public static boolean itemGroup = true;

    @SuppressWarnings("unused")
    public enum EntityRestriction {
        ANY { public boolean canPickup(Player player, Entity target) {
            return true;
        } },
        LIVING { public boolean canPickup(Player player, Entity target) {
            return target.showVehicleHealth();
        } },
        NON_BOSS { public boolean canPickup(Player player, Entity target) {
            return target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES);
        } },
        NON_MONSTER { public boolean canPickup(Player player, Entity target) {
            return target.showVehicleHealth() && !(target instanceof Enemy);
        } },
        NOT_AGGROED { public boolean canPickup(Player player, Entity target) {
            return target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof Mob mobEntity) || getTarget(mobEntity) == null);
        } },
        NOT_AGGROED_PLAYER { public boolean canPickup(Player player, Entity target) {
            return target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof Mob mobEntity) || getTarget(mobEntity) != player);
        } },
        ANIMAL { public boolean canPickup(Player player, Entity target) {
            return target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature;
        } },
        OWNED { public boolean canPickup(Player player, Entity target) {
            return target instanceof OwnableEntity tameable && tameable.getOwner() == player;
        } };

        public abstract boolean canPickup(Player player, Entity target);

        private static @Nullable LivingEntity getTarget(Mob mobEntity) {
            var target = mobEntity.getTarget();
            if (target != null)
                return target.isAlive() ? target : null;
            var targetOptional = mobEntity.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET);
            //noinspection OptionalAssignedToNull
            if (targetOptional == null || targetOptional.isEmpty()) return null;
            var targetActual = targetOptional.get();
            return targetActual.isAlive() ? targetActual : null;
        }
    }
}
