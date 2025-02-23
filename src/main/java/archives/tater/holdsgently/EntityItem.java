package archives.tater.holdsgently;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

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
            "Invulnerable",
            "PortalCooldown",
            "TicksFrozen",
            "Glowing",
            "HurtTime",
            "SleepingX",
            "SleepingY",
            "SleepingZ"
    };

    public static final String UNINITIALIZED = "Unitialized";

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

        var entityType = getEntityType(stack.getNbt());

        if (entityType == null) return ActionResult.CONSUME;

        var nbt = stack.getNbt();
        if (nbt.getBoolean(UNINITIALIZED)) {
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
            var entity = EntityType.getEntityFromNbt(nbt.getCompound(EntityType.ENTITY_TAG_KEY), world).orElse(null);
            if (entity == null) return ActionResult.CONSUME;
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0f, 0f);
            world.spawnEntity(entity);
        }

        stack.decrement(1);
        world.emitGameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, pos);

        return ActionResult.CONSUME;
    }

    @Override
    public Text getName(ItemStack stack) {
        var type = getEntityType(stack.getNbt());
        return type == null ? super.getName(stack) : type.getName();
    }

    public static @Nullable EntityType<?> getEntityType(@Nullable NbtCompound nbt) {
        if (nbt == null || !nbt.contains(EntityType.ENTITY_TAG_KEY, NbtElement.COMPOUND_TYPE)) return null;
        var entityTag = nbt.getCompound(EntityType.ENTITY_TAG_KEY);
        if (!entityTag.contains(Entity.ID_KEY, NbtElement.STRING_TYPE)) return null;
        return EntityType.get(entityTag.getString(Entity.ID_KEY)).orElse(null);
    }

    public static ItemStack from(Entity entity) {
        var nbt = new NbtCompound();
        if (!entity.saveSelfNbt(nbt)) return ItemStack.EMPTY;
        for (String key : REMOVE_TAGS) {
            nbt.remove(key);
        }
        var stack = HoldsGently.ENTITY_ITEM.getDefaultStack();
        stack.setSubNbt(EntityType.ENTITY_TAG_KEY, nbt);
        if (entity.hasCustomName())
            stack.setCustomName(entity.getCustomName());
        return stack;
    }

    public static ItemStack fromType(EntityType<?> entityType) {
        var stack = HoldsGently.ENTITY_ITEM.getDefaultStack();
        var entityTag = new NbtCompound();
        entityTag.putString(Entity.ID_KEY, Registries.ENTITY_TYPE.getId(entityType).toString());
        stack.setSubNbt(EntityType.ENTITY_TAG_KEY, entityTag);
        stack.getOrCreateNbt().putBoolean(UNINITIALIZED, true);
        return stack;
    }

    public static @Nullable Entity entityOf(ItemStack stack, World world) {
        var entityTag = stack.getSubNbt(EntityType.ENTITY_TAG_KEY);
        if (entityTag == null) return null;
        return EntityType.getEntityFromNbt(entityTag, world).orElse(null);
    }
}
