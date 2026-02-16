package archives.tater.gentlyholds;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.*;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.NamingSchemes;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueList;
import org.jetbrains.annotations.Nullable;

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

        public boolean canPickup(PlayerEntity player, Entity target) {
            return switch (this) {
                case ANY -> true;
                case LIVING -> target instanceof LivingEntity;
                case NON_BOSS -> target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES);
                case NON_MONSTER -> target.isLiving() && !(target instanceof Monster);
                case NOT_AGGROED -> target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof MobEntity mobEntity) || getTarget(mobEntity) == null);
                case NOT_AGGROED_PLAYER -> target.isLiving() && !target.getType().isIn(ConventionalEntityTypeTags.BOSSES) && (!(target instanceof MobEntity mobEntity) || getTarget(mobEntity) != player);
                case ANIMAL -> target instanceof AnimalEntity || target instanceof WaterCreatureEntity || target instanceof AmbientEntity;
                case OWNED -> target instanceof Tameable tameable && tameable.getOwner() == player;
            };
        }

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
