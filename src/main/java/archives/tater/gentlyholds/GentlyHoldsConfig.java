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

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.*;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.NamingSchemes;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class GentlyHoldsConfig extends WrappedConfig {
    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    public EntityRestriction entityRestriction = EntityRestriction.NON_MONSTER;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @Comment("Maximum width to pick up entity")
    @FloatRange(min = 0, max = 16)
    public float maxWidth = 2;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @Comment("Maximum height to pick up entity")
    @FloatRange(min = 0, max = 16)
    public float maxHeight = 2;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @Comment("Hands must be empty")
    public boolean emptyHands = true;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @Comment("Entities are spawned when dropped in the world")
    public boolean spawnDrop = true;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @ChangeWarning(Type.RequiresRestart)
    @Comment("Entities can be equipped in head slot\nRequires restart")
    public boolean canWearHat = true;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @ChangeWarning(Type.RequiresRestart)
    @Comment("Add creative mode tab\nRequires reloading world")
    public boolean creativeTab = true;

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @ChangeWarning(Type.RequiresRestart)
    @Comment("Entities that can always be picked up\nRequires restart")
    public List<String> entityWhitelist = ValueList.create("minecraft:creeper");

    @DisplayNameConvention(NamingSchemes.TITLE_CASE)
    @SerializedNameConvention(NamingSchemes.SNAKE_CASE)
    @ChangeWarning(Type.RequiresRestart)
    @Comment("Entities that can can never be picked up\nRequires restart")
    public List<String> entityBlacklist = ValueList.create("minecraft:creeper");


    @SuppressWarnings("unused")
    public enum EntityRestriction {
        ANY,
        LIVING,
        NON_BOSS,
        NON_MONSTER,
        NOT_AGGROED,
        NOT_AGGROED_PLAYER,
        ANIMAL,
        OWNED;

        public boolean canPickup(Player player, Entity target) {
            return switch (this) {
                case ANY -> true;
                case LIVING -> target instanceof LivingEntity;
                case NON_BOSS -> target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES);
                case NON_MONSTER -> target.showVehicleHealth() && !(target instanceof Enemy);
                case NOT_AGGROED -> target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof Mob mobEntity) || getTarget(mobEntity) == null);
                case NOT_AGGROED_PLAYER -> target.showVehicleHealth() && !target.getType().is(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof Mob mobEntity) || getTarget(mobEntity) != player);
                case ANIMAL -> target instanceof Animal || target instanceof WaterAnimal || target instanceof AmbientCreature;
                case OWNED -> target instanceof OwnableEntity tameable && tameable.getOwner() == player;
            };
        }

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
