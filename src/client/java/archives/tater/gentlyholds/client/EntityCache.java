package archives.tater.gentlyholds.client;

import archives.tater.gentlyholds.EntityItem;
import archives.tater.gentlyholds.GentlyHolds;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
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
    private static final Map<Level, Map<TypedEntityData<EntityType<?>>, @Nullable Entity>> ENTITY_CACHE = new WeakHashMap<>();
    private static final Map<EntityRenderDispatcher, Map<Level, Map<TypedEntityData<EntityType<?>>, @Nullable EntityRenderState>>> RENDER_STATE_CACHE = new WeakHashMap<>();

    public static @Nullable EntityRenderState get(ItemStack stack, Level level, EntityRenderDispatcher entities) {
        var entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData == null) return null;
        return get(entityData, level, entities);
    }

    public static @Nullable EntityRenderState get(TypedEntityData<EntityType<?>> data, Level level, EntityRenderDispatcher entities) {
        if (!GentlyHolds.CONFIG.animated)
            return RENDER_STATE_CACHE
                    .computeIfAbsent(entities, _ -> new WeakHashMap<>())
                    .computeIfAbsent(level, _ -> new WeakHashMap<>())
                    .computeIfAbsent(data, data2 -> createRenderState(EntityItem.fakeEntityOf(data2, level), entities));

        return createRenderState(ENTITY_CACHE
                .computeIfAbsent(level, _ -> new WeakHashMap<>())
                .computeIfAbsent(data, data2 -> EntityItem.fakeEntityOf(data2, level)),
                entities
        );
    }

    private static <T extends Entity> @Nullable EntityRenderState createRenderState(@Nullable T entity, EntityRenderDispatcher entities) {
        if (entity == null) return null;
        return createRenderState(entity, entities.getRenderer(entity));
    }

    private static <T extends Entity, S extends EntityRenderState> S createRenderState(T entity, EntityRenderer<T, S> renderer) {
        var state = renderer.createRenderState();
        renderer.extractRenderState((T) entity, state, 1f);
        return state;
    }
}
