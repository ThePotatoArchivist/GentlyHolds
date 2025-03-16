package archives.tater.gentlyholds;

import com.mojang.serialization.MapCodec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Unit;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class EntityItem extends Item {

    public static final String[] REMOVE_TAGS = {
            Entity.UUID_KEY,
            "Pos",
            "Rotation",
            "Motion",
            "FallDistance",
            "Fire",
            "Air",
            "OnGround",
            "HurtByTimestamp",
            "Invulnerable",
            "PortalCooldown",
            "TicksFrozen",
            "Glowing",
            "HurtTime",
            "SleepingX",
            "SleepingY",
            "SleepingZ"
    };

    private static final MapCodec<EntityType<?>> ENTITY_TYPE_MAP_CODEC = Registries.ENTITY_TYPE.getCodec().fieldOf("id");

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

        var entityType = getEntityType(stack.get(DataComponentTypes.ENTITY_DATA));

        if (entityType == null) return ActionResult.CONSUME;

        if (stack.contains(GentlyHolds.UNINITIALIZED)) {
            if (entityType.spawnFromItemStack(
                    serverWorld,
                    stack,
                    context.getPlayer(),
                    targetPos,
                    SpawnReason.SPAWN_EGG,
                    true,
                    pos != targetPos && direction == Direction.UP
            ) == null) return ActionResult.CONSUME;
        } else {
            var nbt = stack.get(DataComponentTypes.ENTITY_DATA);
            if (nbt == null) return ActionResult.FAIL;
            var entity = EntityType.getEntityFromNbt(nbt.copyNbt(), world).orElse(null);
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
        var type = getEntityType(stack.get(DataComponentTypes.ENTITY_DATA));
        return type == null ? super.getName(stack) : type.getName();
    }

    public static @Nullable EntityType<?> getEntityType(@Nullable NbtComponent nbt) {
        return nbt == null || nbt.isEmpty() ? null : nbt.get(ENTITY_TYPE_MAP_CODEC).result().orElse(null);
    }

    public static ItemStack from(Entity entity) {
        var nbt = new NbtCompound();
        if (!entity.saveSelfNbt(nbt)) return ItemStack.EMPTY;
        for (String key : REMOVE_TAGS) {
            nbt.remove(key);
        }
        var stack = GentlyHolds.ENTITY_ITEM.getDefaultStack();
        NbtComponent.set(DataComponentTypes.ENTITY_DATA, stack, nbt);
        if (entity.hasCustomName())
            stack.set(DataComponentTypes.CUSTOM_NAME, entity.getCustomName());
        return stack;
    }

    public static ItemStack fromType(EntityType<?> entityType) {
        var stack = GentlyHolds.ENTITY_ITEM.getDefaultStack();
        var entityTag = new NbtCompound();
        entityTag.putString(Entity.ID_KEY, Registries.ENTITY_TYPE.getId(entityType).toString());
        NbtComponent.set(DataComponentTypes.ENTITY_DATA, stack, entityTag);
        stack.set(GentlyHolds.UNINITIALIZED, Unit.INSTANCE);
        return stack;
    }

    public static @Nullable Entity entityOf(ItemStack stack, World world) {
        var entityCompound = stack.get(DataComponentTypes.ENTITY_DATA);
        if (entityCompound == null) return null;
        return EntityType.getEntityFromNbt(entityCompound.copyNbt(), world).orElse(null);
    }
}
