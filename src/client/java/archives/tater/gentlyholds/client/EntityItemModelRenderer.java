package archives.tater.gentlyholds.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

public record EntityItemModelRenderer(
        EntityRenderDispatcher entityRenderDispatcher,
        boolean rotated, // displayContext == ItemDisplayContext.FIXED
        boolean centered, // displayContext == ItemDisplayContext.FIXED || displayContext == ItemDisplayContext.GUI;
        boolean shrink // displayContext != ItemDisplayContext.HEAD
) implements SpecialModelRenderer<EntityRenderState> {

    private static final CameraRenderState CAMERA_RENDER_STATE = new CameraRenderState();

    @Override
    public void submit(@Nullable EntityRenderState argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (argument == null) return;
        argument.lightCoords = lightCoords;
        var rotated = this.rotated && argument.boundingBoxWidth * 1.5f >= argument.boundingBoxHeight;
        var centered = !rotated && this.centered;
        poseStack.pushPose();
        poseStack.translate(0.5, rotated || centered ? 0.5 : 0.0, 0.5);
        if (rotated) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
        if (shrink) {
            var scale = 1 / max(1, max(argument.boundingBoxWidth, argument.boundingBoxHeight));
            poseStack.scale(scale, scale, scale);
        }
        if (centered)
            poseStack.translate(0, -argument.boundingBoxHeight / 2, 0);
        if (rotated)
            poseStack.translate(0, -1 / 16.0, 0);
        entityRenderDispatcher.submit(argument, CAMERA_RENDER_STATE, 0.0, 0.0, 0.0, poseStack, submitNodeCollector);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {

    }

    @Override
    public @Nullable EntityRenderState extractArgument(ItemStack stack) {
        var entity = EntityCache.get(stack, requireNonNull(Minecraft.getInstance().level));
        if (entity == null) return null;
        @SuppressWarnings("unchecked")
        var renderer = (EntityRenderer<Entity, EntityRenderState>) entityRenderDispatcher.getRenderer(entity);
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, 1f);
        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null)
            state.ageInTicks = camera.tickCount + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return state;
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked(
            boolean rotated, // displayContext == ItemDisplayContext.FIXED
            boolean centered, // displayContext == ItemDisplayContext.FIXED || displayContext == ItemDisplayContext.GUI;
            boolean shrink // displayContext != ItemDisplayContext.HEAD
    ) implements SpecialModelRenderer.Unbaked<EntityRenderState> {
        public static final MapCodec<EntityItemModelRenderer.Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.BOOL.fieldOf("rotated").forGetter(Unbaked::rotated),
                Codec.BOOL.fieldOf("centered").forGetter(Unbaked::centered),
                Codec.BOOL.fieldOf("shrink").forGetter(Unbaked::shrink)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<EntityItemModelRenderer.Unbaked> type() {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<EntityRenderState> bake(BakingContext context) {
            return new EntityItemModelRenderer(Minecraft.getInstance().getEntityRenderDispatcher(), rotated, centered, shrink);
        }
    }
}
