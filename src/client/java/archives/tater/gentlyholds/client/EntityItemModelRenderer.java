package archives.tater.gentlyholds.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class EntityItemModelRenderer implements SpecialModelRenderer<EntityRenderState> {

    private final EntityRenderDispatcher entityRenderDispatcher;

    private static final CameraRenderState UNKNOWN_OBJECT = new CameraRenderState();

    public EntityItemModelRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        this.entityRenderDispatcher = entityRenderDispatcher;
    }

    @Override
    public void submit(@Nullable EntityRenderState data, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, boolean glint, int i) {
        if (data == null) return;
        data.lightCoords = light;
        var rotated = displayContext == ItemDisplayContext.FIXED && data.boundingBoxWidth * 1.5f >= data.boundingBoxHeight;
        var centered = !rotated && displayContext == ItemDisplayContext.FIXED || displayContext == ItemDisplayContext.GUI;
        poseStack.pushPose();
        poseStack.translate(0.5, rotated || centered ? 0.5 : 0.0, 0.5);
        if (rotated) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
        if (displayContext != ItemDisplayContext.HEAD) {
            var scale = 1 / Math.max(1, Math.max(data.boundingBoxWidth, data.boundingBoxHeight));
            poseStack.scale(scale, scale, scale);
        }
        if (centered)
            poseStack.translate(0, -data.boundingBoxHeight / 2, 0);
        if (rotated)
            poseStack.translate(0, -1 / 16.0, 0);
        entityRenderDispatcher.submit(data, UNKNOWN_OBJECT, 0.0, 0.0, 0.0, poseStack, submitNodeCollector);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {

    }

    @Override
    public @Nullable EntityRenderState extractArgument(ItemStack stack) {
        var data = EntityCache.get(stack, requireNonNull(Minecraft.getInstance().level));
        if (data == null) return null;
        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null)
            data.ageInTicks = camera.tickCount + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return data;
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<EntityItemModelRenderer.Unbaked> CODEC = MapCodec.unit(new EntityItemModelRenderer.Unbaked());

        @Override
        public MapCodec<EntityItemModelRenderer.Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(BakingContext context) {
            return new EntityItemModelRenderer(Minecraft.getInstance().getEntityRenderDispatcher());
        }
    }
}
