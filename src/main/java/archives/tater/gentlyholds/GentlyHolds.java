package archives.tater.gentlyholds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalEntityTypeTags;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GentlyHolds implements ModInitializer {
	public static final String MOD_ID = "gentlyholds";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

    public static final GentlyHoldsConfig CONFIG = GentlyHoldsConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", MOD_ID, GentlyHoldsConfig.class);

    private static @Nullable Set<EntityType<?>> entityWhitelist;
    private static @Nullable Set<EntityType<?>> entityBlacklist;

    public static final ComponentType<Unit> UNINITIALIZED = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			id("uninitialized"),
			ComponentType.<Unit>builder().codec(Unit.CODEC).packetCodec(PacketCodec.unit(Unit.INSTANCE)).build()
	);

	public static final TagKey<EntityType<?>> MISC_LIVING = TagKey.of(RegistryKeys.ENTITY_TYPE, id("misc_living"));

	public static final Item ENTITY_ITEM = Registry.register(
			Registries.ITEM,
			id("entity_item"),
			new EntityItem(new Item.Settings().equipmentSlot((livingEntity, stack) -> CONFIG.canWearHat ? EquipmentSlot.HEAD : EquipmentSlot.MAINHAND))
	);

	public static final ItemGroup ENTITIES = Registry.register(
			Registries.ITEM_GROUP,
			id("entities"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup." + MOD_ID + ".entities"))
					.icon(() -> EntityItem.fromType(EntityType.PIG))
					.entries((displayContext, entries) -> {
						if (!CONFIG.creativeTab) return;
						var spawnEggTypes = StreamSupport.stream(SpawnEggItem.getAll().spliterator(), false).map(spawnEggItem -> spawnEggItem.getEntityType(ENTITY_ITEM.getDefaultStack())).toList();
						Registries.ENTITY_TYPE.forEach(entityType -> {
							if (entityType.isSaveable() && entityType.getSpawnGroup() != SpawnGroup.MISC && !entityType.isIn(ConventionalEntityTypeTags.BOSSES) && (spawnEggTypes.contains(entityType) || entityType.isIn(MISC_LIVING)))
								entries.add(EntityItem.fromType(entityType));
						});
					})
					.build()
	);

	private static Set<EntityType<?>> loadIds(Collection<String> rawIds) {
		return rawIds.stream()
				.map(Identifier::tryParse)
				.filter(Objects::nonNull)
				.map(Registries.ENTITY_TYPE::getOrEmpty)
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
	}

	public static boolean canPickup(PlayerEntity player, Entity target) {
		if (entityBlacklist == null) entityBlacklist = loadIds(CONFIG.entityBlacklist);
		if (entityWhitelist == null) entityWhitelist = loadIds(CONFIG.entityWhitelist);

		return entityWhitelist.contains(target.getType()) ||
				!entityBlacklist.contains(target.getType())
						&& target.getWidth() <= CONFIG.maxWidth
						&& target.getHeight() <= CONFIG.maxHeight
						&& CONFIG.entityRestriction.canPickup(player, target);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		UseEntityCallback.EVENT.register((playerEntity, world, hand, entity, entityHitResult) -> {
			if (!playerEntity.shouldCancelInteraction()) return ActionResult.PASS;
			if (entity instanceof PlayerEntity) return ActionResult.PASS;
			if (CONFIG.emptyHands && (!playerEntity.getMainHandStack().isEmpty() || !playerEntity.getOffHandStack().isEmpty())) return ActionResult.PASS;
			var targetEntity = entity instanceof EnderDragonPart part ? part.owner : entity;
			if (!canPickup(playerEntity, targetEntity)) return ActionResult.PASS;
			if (world.isClient) return ActionResult.SUCCESS;

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
