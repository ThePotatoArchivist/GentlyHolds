package archives.tater.gentlyholds;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class EntityItem extends Item {

    public static final String[] REMOVE_TAGS = {
            Entity.UUID_KEY,
            Entity.POS_KEY,
            Entity.ROTATION_KEY,
            Entity.MOTION_KEY,
            Entity.FALL_DISTANCE_KEY,
            Entity.FIRE_KEY,
            Entity.AIR_KEY,
            Entity.ON_GROUND_KEY,
            Entity.INVULNERABLE_KEY,
            Entity.PORTAL_COOLDOWN_KEY,
            "TicksFrozen",
            "HasVisualFire",
            Entity.GLOWING_KEY,
            LivingEntity.HURT_TIME_KEY,
            LivingEntity.HURT_BY_TIMESTAMP_KEY,
            LivingEntity.SLEEPING_POS_KEY
    };

    public EntityItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.SUCCESS;

        var stack = context.getStack();
        var pos = context.getBlockPos();
        var direction = context.getSide();
        var state = world.getBlockState(pos);

        var targetPos = state.getCollisionShape(world, pos).isEmpty() ? pos : pos.offset(direction);

        var data = stack.get(DataComponentTypes.ENTITY_DATA);

        if (data == null) return ActionResult.CONSUME;

        if (stack.contains(GentlyHolds.UNINITIALIZED)) {
            if (data.getType().spawnFromItemStack(
                    serverWorld,
                    stack,
                    context.getPlayer(),
                    targetPos,
                    SpawnReason.SPAWN_ITEM_USE,
                    true,
                    pos != targetPos && direction == Direction.UP
            ) == null) return ActionResult.CONSUME;
        } else {
            var entity = entityOf(stack, world);
            if (entity == null) return ActionResult.CONSUME;
            entity.refreshPositionAndAngles(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, wrapDegrees(world.random.nextFloat() * 360), 0f);
            if (entity instanceof MobEntity mobEntity) {
                mobEntity.headYaw = mobEntity.getYaw();
                mobEntity.bodyYaw = mobEntity.getYaw();
                mobEntity.playAmbientSound();
            }
            world.spawnEntity(entity);
        }

        stack.decrement(1);
        world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);

        return ActionResult.CONSUME;
    }

    @Override
    public Text getName(ItemStack stack) {
        var data = stack.get(DataComponentTypes.ENTITY_DATA);
        return data == null ? super.getName(stack) : data.getType().getName();
    }

    public static ItemStack from(Entity entity) {
        NbtWriteView writeView;
        try (var logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), GentlyHolds.LOGGER)) {
            writeView = NbtWriteView.create(logging, entity.getRegistryManager());
            if (!entity.saveSelfData(writeView)) return ItemStack.EMPTY;
        }

        for (String key : REMOVE_TAGS)
            writeView.remove(key);

        var stack = GentlyHolds.ENTITY_ITEM.getDefaultStack();
        stack.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(entity.getType(), writeView.getNbt()));
        if (entity.hasCustomName())
            stack.set(DataComponentTypes.CUSTOM_NAME, entity.getCustomName());
        return stack;
    }

    public static ItemStack fromType(EntityType<?> entityType) {
        var stack = GentlyHolds.ENTITY_ITEM.getDefaultStack();
        stack.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(entityType, new NbtCompound()));
        stack.set(GentlyHolds.UNINITIALIZED, Unit.INSTANCE);
        return stack;
    }

    public static @Nullable Entity entityOf(ItemStack stack, World world) {
        var entityData = stack.get(DataComponentTypes.ENTITY_DATA);
        if (entityData == null) return null;
        return entityOf(entityData, world);
    }

    public static @Nullable Entity entityOf(TypedEntityData<EntityType<?>> entityData, World world) {
        var entity = entityData.getType().create(world, SpawnReason.SPAWN_ITEM_USE);
        if (entity == null) return null;
        entityData.applyToEntity(entity);
        return entity;
    }
}
