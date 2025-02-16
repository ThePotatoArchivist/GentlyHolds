package archives.tater.holdsgently;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HoldsGently implements ModInitializer {
	public static final String MOD_ID = "holdsgently";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public static final TagKey<EntityType<?>> MISC_LIVING = TagKey.of(RegistryKeys.ENTITY_TYPE, id("misc_living"));

	public static final Item ENTITY_ITEM = Registry.register(
			Registries.ITEM,
			id("entity_item"),
			new EntityItem(new FabricItemSettings())
	);

	public static final ItemGroup ENTITIES = Registry.register(
			Registries.ITEM_GROUP,
			id("entities"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup." + MOD_ID + ".entities"))
					.icon(() -> EntityItem.fromType(EntityType.PIG))
					.entries((displayContext, entries) -> {
						Registries.ENTITY_TYPE.forEach(entityType -> {
							if (entityType.isSaveable() && (entityType.isIn(MISC_LIVING) || entityType.getSpawnGroup() != SpawnGroup.MISC))
								entries.add(EntityItem.fromType(entityType));
						});
					})
					.build()
	);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
			if (!playerEntity.shouldCancelInteraction()) return ActionResult.PASS;
			if (entity instanceof PlayerEntity) return ActionResult.PASS;
			if (world.isClient) return ActionResult.SUCCESS;
			var targetEntity = entity instanceof EnderDragonPart part ? part.owner : entity;
			var stack = EntityItem.from(targetEntity);
			if (playerEntity.getStackInHand(hand).isEmpty())
				playerEntity.setStackInHand(hand, stack);
			else if (!playerEntity.giveItemStack(stack))
				playerEntity.dropStack(stack);
			targetEntity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
			return ActionResult.SUCCESS;
		});
	}
}
