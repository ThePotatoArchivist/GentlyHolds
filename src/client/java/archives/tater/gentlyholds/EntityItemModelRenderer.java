package archives.tater.gentlyholds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class EntityItemModelRenderer implements SpecialModelRenderer<EntityRenderState> {

    private final EntityRenderManager entityRenderDispatcher;

    private static final CameraRenderState UNKNOWN_OBJECT = new CameraRenderState();

    public EntityItemModelRenderer(EntityRenderManager entityRenderDispatcher) {
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public void render(@Nullable EntityRenderState data, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
        if (data == null) return;
        data.light = light;
        var rotated = displayContext == ItemDisplayContext.FIXED && data.width * 1.5f >= data.height;
        var centered = !rotated && displayContext == ItemDisplayContext.FIXED || displayContext == ItemDisplayContext.GUI;
        matrices.push();
        matrices.translate(0.5, rotated || centered ? 0.5 : 0.0, 0.5);
        if (rotated) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        }
        if (displayContext != ItemDisplayContext.HEAD) {
            var scale = 1 / Math.max(1, Math.max(data.width, data.height));
            matrices.scale(scale, scale, scale);
        }
        if (centered)
            matrices.translate(0, -data.height / 2, 0);
        if (rotated)
            matrices.translate(0, -1 / 16.0, 0);
        entityRenderDispatcher.render(data, UNKNOWN_OBJECT, 0.0, 0.0, 0.0, matrices, queue);
        matrices.pop();
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> vertices) {

    }

    @Override
    public EntityRenderState getData(ItemStack stack) {
        var data = EntityCache.get(stack, MinecraftClient.getInstance().world);
        if (data == null) return null;
        var camera = MinecraftClient.getInstance().getCameraEntity();
        if (camera != null)
            data.age = camera.age + MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        return data;
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<EntityItemModelRenderer.Unbaked> CODEC = MapCodec.unit(new EntityItemModelRenderer.Unbaked());

        @Override
        public MapCodec<EntityItemModelRenderer.Unbaked> getCodec() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(BakeContext context) {
            return new EntityItemModelRenderer(MinecraftClient.getInstance().getEntityRenderDispatcher());
        }
    }
}
