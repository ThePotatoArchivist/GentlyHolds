package archives.tater.gentlyholds.mixin;

import archives.tater.gentlyholds.EntityItem;
import archives.tater.gentlyholds.GentlyHolds;
import archives.tater.gentlyholds.GentlyHoldsConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import static net.minecraft.util.math.MathHelper.atan2;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getStack();

    @Shadow
    private int itemAge;

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void spawnEntity(CallbackInfo ci) {
        if (getWorld().isClient || itemAge != 0) return;
        var stack = getStack();
        if (!GentlyHoldsConfig.spawnDrop || !stack.isOf(GentlyHolds.ENTITY_ITEM)) return;
        var entity = EntityItem.entityOf(stack, getWorld());
        if (entity == null) return;
        var velocity = getVelocity();
        var yaw = atan2(velocity.y, velocity.x);
        entity.refreshPositionAndAngles(getX(), getY(), getZ(), Double.isNaN(yaw) ? 0f : (float) yaw, 0f);
        entity.setVelocity(velocity);
        getWorld().spawnEntity(entity);
        discard();
        ci.cancel();
    }
}
