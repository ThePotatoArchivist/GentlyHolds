package archives.tater.gentlyholds.client;

import archives.tater.gentlyholds.EntityItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityCache {
    private static final Map<Level, Map<TypedEntityData<EntityType<?>>, @Nullable Entity>> CACHE = new WeakHashMap<>();

    public static @Nullable Entity get(ItemStack stack, Level level) {
        var entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData == null) return null;
        return get(entityData, level);
    }

    public static @Nullable Entity get(TypedEntityData<EntityType<?>> data, Level level) {
        return CACHE
                .computeIfAbsent(level, _w -> new WeakHashMap<>())
                .computeIfAbsent(data, _d -> EntityItem.entityOf(data, level));
    }
}
