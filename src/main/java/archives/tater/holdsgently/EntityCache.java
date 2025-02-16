package archives.tater.holdsgently;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityCache {
    private static final Map<ItemStack, Entity> CACHE = new WeakHashMap<>();

    public static Entity get(ItemStack stack, World world) {
        if (CACHE.containsKey(stack) && CACHE.get(stack) == null) return null;
        return CACHE.computeIfAbsent(stack, stack2 -> EntityItem.entityOf(stack2, world));
    }
}
