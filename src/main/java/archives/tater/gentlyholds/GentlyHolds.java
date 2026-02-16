package archives.tater.gentlyholds;

//import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.Equippable;
import eu.midnightdust.lib.config.MidnightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.StreamSupport;

public class GentlyHolds implements ModInitializer {
	public static final String MOD_ID = "gentlyholds";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static final DataComponentType<Unit> UNINITIALIZED = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			id("uninitialized"),
			DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)).build()
	);

	public static final TagKey<EntityType<?>> MISC_LIVING = TagKey.create(Registries.ENTITY_TYPE, id("misc_living"));

	public static final Item ENTITY_ITEM = Items.registerItem(
			ResourceKey.create(Registries.ITEM, id("entity_item")),
			EntityItem::new,
			new Item.Properties()
	);

	public static final CreativeModeTab ENTITIES = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			id("entities"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup." + MOD_ID + ".entities"))
					.icon(() -> EntityItem.fromType(EntityType.CREEPER))
					.displayItems((displayContext, entries) -> {
						if (!GentlyHoldsConfig.itemGroup) return;
						var spawnEggTypes = StreamSupport.stream(SpawnEggItem.eggs().spliterator(), false).map(spawnEggItem -> spawnEggItem.getType(spawnEggItem.getDefaultInstance())).toList();
						BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
							if (entityType.canSerialize() && entityType.getCategory() != MobCategory.MISC && !entityType.is(ConventionalEntityTypeTags.BOSSES) && (spawnEggTypes.contains(entityType) || entityType.is(MISC_LIVING)))
								entries.accept(EntityItem.fromType(entityType));
						});
					})
					.build()
	);

	public static boolean canPickup(Player player, Entity target) {
		return target.getBbWidth() <= GentlyHoldsConfig.maxWidth && target.getBbHeight() <= GentlyHoldsConfig.maxHeight && GentlyHoldsConfig.entityRestriction.canPickup(player, target);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		MidnightConfig.init(MOD_ID, GentlyHoldsConfig.class);

		if (GentlyHoldsConfig.canWearHat)
			DefaultItemComponentEvents.MODIFY.register(context -> {
				context.modify(ENTITY_ITEM, builder -> builder.set(
						DataComponents.EQUIPPABLE,
						Equippable.builder(EquipmentSlot.HEAD).setSwappable(false).build()
				));
			});

		UseEntityCallback.EVENT.register((playerEntity, level, hand, entity, entityHitResult) -> {
			if (!playerEntity.isSecondaryUseActive()) return InteractionResult.PASS;
			if (entity instanceof Player) return InteractionResult.PASS;
			if (GentlyHoldsConfig.emptyHands && (!playerEntity.getMainHandItem().isEmpty() || !playerEntity.getOffhandItem().isEmpty())) return InteractionResult.PASS;
			var targetEntity = entity instanceof EnderDragonPart part ? part.parentMob : entity;
			if (!canPickup(playerEntity, targetEntity)) return InteractionResult.PASS;
			if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

			var stack = EntityItem.from(targetEntity);
			if (playerEntity.getItemInHand(hand).isEmpty())
				playerEntity.setItemInHand(hand, stack);
			else if (!playerEntity.addItem(stack))
				playerEntity.spawnAtLocation(serverLevel, stack);
			targetEntity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
			return InteractionResult.SUCCESS;
		});
	}
}
