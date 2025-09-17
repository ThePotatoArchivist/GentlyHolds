package archives.tater.gentlyholds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Map;
import java.util.WeakHashMap;

public class EntityCache {
    private static final Map<World, Map<TypedEntityData<EntityType<?>>, EntityRenderState>> CACHE = new WeakHashMap<>();

    public static EntityRenderState get(ItemStack stack, World world) {
        var entityData = stack.get(DataComponentTypes.ENTITY_DATA);
        if (entityData == null) return null;
        return get(entityData, world);
    }

    public static EntityRenderState get(TypedEntityData<EntityType<?>> data, World world) {
        return CACHE
                .computeIfAbsent(world, _w -> new WeakHashMap<>())
                .computeIfAbsent(data, nbt1 -> {
                    var entity = EntityItem.entityOf(data, world);
                    @SuppressWarnings("unchecked")
                    var renderer = (EntityRenderer<Entity, EntityRenderState>) MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
                    var state = renderer.createRenderState();
                    renderer.updateRenderState(entity, state, 1f);
                    return state;
                });
    }
}
