package archives.tater.gentlyholds;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;

public class GentlyHoldsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		SpecialModelTypes.ID_MAPPER.put(GentlyHolds.id("entity"), EntityItemModelRenderer.Unbaked.CODEC);
	}
}
