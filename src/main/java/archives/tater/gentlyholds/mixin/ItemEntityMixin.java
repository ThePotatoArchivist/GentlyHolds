package archives.tater.gentlyholds.mixin;

import archives.tater.gentlyholds.EntityItem;
import archives.tater.gentlyholds.GentlyHolds;
import archives.tater.gentlyholds.GentlyHoldsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.util.Mth.atan2;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    private int age;

    @Inject(
            method = "tick",
            at = @At("HEAD"),
            cancellable = true
    )
    private void spawnEntity(CallbackInfo ci) {
        if (level().isClientSide() || age != 0) return;
        var stack = getItem();
        if (!GentlyHolds.CONFIG.spawnDrop || !stack.is(GentlyHolds.ENTITY_ITEM)) return;
        var entity = EntityItem.entityOf(stack, level());
        if (entity == null) return;
        var velocity = getDeltaMovement();
        var yaw = atan2(velocity.y, velocity.x);
        entity.snapTo(getX(), getY(), getZ(), Double.isNaN(yaw) ? 0f : (float) yaw, 0f);
        entity.setDeltaMovement(velocity);
        level().addFreshEntity(entity);
        discard();
        ci.cancel();
    }
}
