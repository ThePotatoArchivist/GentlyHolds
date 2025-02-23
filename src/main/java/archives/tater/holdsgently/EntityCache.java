package archives.tater.holdsgently;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityCache {
    private static final Map<World, Map<ItemStack, Entity>> CACHE = new WeakHashMap<>();

    public static Entity get(ItemStack stack, World world) {
        return CACHE
                .computeIfAbsent(world, _w -> new WeakHashMap<>())
                .computeIfAbsent(stack, _s -> EntityItem.entityOf(stack, world));
    }
}
