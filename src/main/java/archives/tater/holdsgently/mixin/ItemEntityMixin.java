package archives.tater.holdsgently.mixin;

import archives.tater.holdsgently.EntityItem;
import archives.tater.holdsgently.HoldsGently;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Shadow public abstract void setStack(ItemStack stack);

	@Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;DDD)V", at = @At("TAIL"))
	private void init(World world, double x, double y, double z, ItemStack stack, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        if (!stack.isOf(HoldsGently.ENTITY_ITEM)) return;
        var entity = EntityItem.entityOf(stack, world);
		if (entity == null) return;
		entity.refreshPositionAndAngles(x, y, z, 0f, 0f);
		entity.setVelocity(8 * velocityX, velocityY, 8 * velocityZ);
        world.spawnEntity(entity);
        setStack(ItemStack.EMPTY);
    }
}
