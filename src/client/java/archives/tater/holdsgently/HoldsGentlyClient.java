package archives.tater.holdsgently;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformationMode;

public class HoldsGentlyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		BuiltinItemRendererRegistry.INSTANCE.register(HoldsGently.ENTITY_ITEM, (stack, mode, matrices, vertexConsumers, light, overlay) -> {
			var entity = EntityCache.get(stack, MinecraftClient.getInstance().world);
			if (entity == null) return;
			var camera = MinecraftClient.getInstance().getCameraEntity();
			if (camera != null)
				entity.age = camera.age;
			var centered = mode == ModelTransformationMode.FIXED || mode == ModelTransformationMode.GUI;
			matrices.push();
            matrices.translate(0.5, centered ? 0.5 : 0.0, 0.5);
			var scale = 1 / Math.max(1, Math.max(entity.getWidth(), entity.getHeight()));
			matrices.scale(scale, scale, scale);
			if (centered)
				matrices.translate(0, -entity.getHeight() / 2, 0);
			MinecraftClient.getInstance().getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, 0f, camera == null ? 0 : MinecraftClient.getInstance().getTickDelta(), matrices, vertexConsumers, light);
			matrices.pop();
		});
	}
}
