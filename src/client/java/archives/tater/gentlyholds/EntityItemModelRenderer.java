package archives.tater.gentlyholds;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Set;

public class EntityItemModelRenderer implements SpecialModelRenderer<NbtComponent> {

    private final EntityRenderDispatcher entityRenderDispatcher;

    public EntityItemModelRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public void render(@Nullable NbtComponent data, ItemDisplayContext displayContext, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, boolean glint) {
        var entity = EntityCache.get(data, MinecraftClient.getInstance().world);
        if (entity == null) return;
        var camera = MinecraftClient.getInstance().getCameraEntity();
        if (camera != null)
            entity.age = camera.age;
        var rotated = displayContext == ItemDisplayContext.FIXED && entity.getWidth() >= entity.getHeight();
        var centered = !rotated && displayContext == ItemDisplayContext.FIXED || displayContext == ItemDisplayContext.GUI;
        matrices.push();
        matrices.translate(0.5, rotated || centered ? 0.5 : 0.0, 0.5);
        if (rotated) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        }
        if (displayContext != ItemDisplayContext.HEAD) {
            var scale = 1 / Math.max(1, Math.max(entity.getWidth(), entity.getHeight()));
            matrices.scale(scale, scale, scale);
        }
        if (centered)
            matrices.translate(0, -entity.getHeight() / 2, 0);
        if (rotated)
            matrices.translate(0, -1 / 16.0, 0);
        entityRenderDispatcher.render(entity, 0.0, 0.0, 0.0, camera == null ? 0 : MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false), matrices, vertexConsumers, light);
        matrices.pop();
    }

    @Override
    public void collectVertices(Set<Vector3f> vertices) {

    }

    @Override
    public @Nullable NbtComponent getData(ItemStack stack) {
        return stack.get(DataComponentTypes.ENTITY_DATA);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<EntityItemModelRenderer.Unbaked> CODEC = MapCodec.unit(new EntityItemModelRenderer.Unbaked());

        @Override
        public MapCodec<EntityItemModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(LoadedEntityModels entityModels) {
            return new EntityItemModelRenderer(MinecraftClient.getInstance().getEntityRenderDispatcher());
        }
    }
}
