package archives.tater.gentlyholds;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityCache {
    private static final Map<World, Map<NbtComponent, Entity>> CACHE = new WeakHashMap<>();

    public static Entity get(ItemStack stack, World world) {
        var entityData = stack.get(DataComponentTypes.ENTITY_DATA);
        if (entityData == null) return null;
        return get(entityData, world);
    }

    public static Entity get(NbtComponent nbt, World world) {
        return CACHE
                .computeIfAbsent(world, _w -> new WeakHashMap<>())
                .computeIfAbsent(nbt, nbt1 -> EntityItem.entityOf(nbt1, world));
    }
}
