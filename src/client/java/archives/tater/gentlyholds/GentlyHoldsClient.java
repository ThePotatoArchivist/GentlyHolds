package archives.tater.gentlyholds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.util.math.RotationAxis;

public class GentlyHoldsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BuiltinItemRendererRegistry.INSTANCE.register(GentlyHolds.ENTITY_ITEM, (stack, mode, matrices, vertexConsumers, light, overlay) -> {
			var entity = EntityCache.get(stack, MinecraftClient.getInstance().world);
			if (entity == null) return;
			var camera = MinecraftClient.getInstance().getCameraEntity();
			if (camera != null)
				entity.age = camera.age;
			var rotated = mode == ModelTransformationMode.FIXED && entity.getWidth() >= entity.getHeight();
			var centered = !rotated && mode == ModelTransformationMode.FIXED || mode == ModelTransformationMode.GUI;
			matrices.push();
            matrices.translate(0.5, rotated || centered ? 0.5 : 0.0, 0.5);
			if (rotated) {
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
			}
			if (mode != ModelTransformationMode.HEAD) {
				var scale = 1 / Math.max(1, Math.max(entity.getWidth(), entity.getHeight()));
				matrices.scale(scale, scale, scale);
			}
			if (centered)
				matrices.translate(0, -entity.getHeight() / 2, 0);
			if (rotated)
				matrices.translate(0, -1 / 16.0, 0);
			MinecraftClient.getInstance().getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, 0f, camera == null ? 0 : MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false), matrices, vertexConsumers, light);
			matrices.pop();
		});
	}
}
