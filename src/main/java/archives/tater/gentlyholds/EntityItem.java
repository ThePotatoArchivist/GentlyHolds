package archives.tater.gentlyholds;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueOutput;

import org.jspecify.annotations.Nullable;

import static net.minecraft.util.Mth.wrapDegrees;

public class EntityItem extends Item {

    public static final String[] REMOVE_TAGS = {
            Entity.TAG_UUID,
            Entity.TAG_POS,
            Entity.TAG_ROTATION,
            Entity.TAG_MOTION,
            Entity.TAG_FALL_DISTANCE,
            Entity.TAG_FIRE,
            Entity.TAG_AIR,
            Entity.TAG_ON_GROUND,
            Entity.TAG_INVULNERABLE,
            Entity.TAG_PORTAL_COOLDOWN,
            "TicksFrozen",
            "HasVisualFire",
            Entity.TAG_GLOWING,
            LivingEntity.TAG_HURT_TIME,
            LivingEntity.TAG_HURT_BY_TIMESTAMP,
            LivingEntity.TAG_SLEEPING_POS
    };

    public EntityItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        var stack = context.getItemInHand();
        var pos = context.getClickedPos();
        var direction = context.getClickedFace();
        var state = level.getBlockState(pos);

        var targetPos = state.getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(direction);

        var data = stack.get(DataComponents.ENTITY_DATA);

        if (data == null) return InteractionResult.CONSUME;

        if (stack.has(GentlyHolds.UNINITIALIZED)) {
            if (data.type().spawn(
                    serverLevel,
                    stack,
                    context.getPlayer(),
                    targetPos,
                    EntitySpawnReason.SPAWN_ITEM_USE,
                    true,
                    pos != targetPos && direction == Direction.UP
            ) == null) return InteractionResult.CONSUME;
        } else {
            var entity = entityOf(stack, level);
            if (entity == null) return InteractionResult.CONSUME;
            entity.snapTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, wrapDegrees(level.getRandom().nextFloat() * 360), 0f);
            if (entity instanceof Mob mob) {
                mob.yHeadRot = mob.getYRot();
                mob.yBodyRot = mob.getYRot();
                mob.playAmbientSound();
            }
            level.addFreshEntity(entity);
        }

        stack.shrink(1);
        level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);

        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        var data = stack.get(DataComponents.ENTITY_DATA);
        return data == null ? super.getName(stack) : data.type().getDescription();
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner.getRandom().nextInt(1000) == 0 && entityOf(itemStack, level) instanceof Mob mob) {
            mob.snapTo(owner.position());
            mob.playAmbientSound();
        }
    }

    public static ItemStack from(Entity entity) {
        TagValueOutput writeView;
        try (var logging = new ProblemReporter.ScopedCollector(entity.problemPath(), GentlyHolds.LOGGER)) {
            writeView = TagValueOutput.createWithContext(logging, entity.registryAccess());
            if (!entity.saveAsPassenger(writeView)) return ItemStack.EMPTY;
        }

        for (String key : REMOVE_TAGS)
            writeView.discard(key);

        var stack = GentlyHolds.ENTITY_ITEM.getDefaultInstance();
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entity.getType(), writeView.buildResult()));
        if (entity.hasCustomName())
            stack.set(DataComponents.CUSTOM_NAME, entity.getCustomName());
        return stack;
    }

    public static ItemStack fromType(EntityType<?> entityType) {
        var stack = GentlyHolds.ENTITY_ITEM.getDefaultInstance();
        stack.set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, new CompoundTag()));
        stack.set(GentlyHolds.UNINITIALIZED, Unit.INSTANCE);
        return stack;
    }

    public static @Nullable Entity entityOf(ItemStack stack, Level level) {
        var entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData == null) return null;
        return entityOf(entityData, level);
    }

    public static @Nullable Entity entityOf(TypedEntityData<EntityType<?>> entityData, Level level) {
        var entity = entityData.type().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (entity == null) return null;
        entityData.loadInto(entity);
        return entity;
    }
}
