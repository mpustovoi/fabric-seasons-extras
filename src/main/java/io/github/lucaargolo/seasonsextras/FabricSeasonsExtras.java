package io.github.lucaargolo.seasonsextras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.block.AirConditioningBlock;
import io.github.lucaargolo.seasonsextras.block.GreenhouseGlassBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonCalendarBlock;
import io.github.lucaargolo.seasonsextras.block.SeasonDetectorBlock;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity.Conditioning;
import io.github.lucaargolo.seasonsextras.blockentities.GreenhouseGlassBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonCalendarBlockEntity;
import io.github.lucaargolo.seasonsextras.blockentities.SeasonDetectorBlockEntity;
import io.github.lucaargolo.seasonsextras.item.*;
import io.github.lucaargolo.seasonsextras.patchouli.FabricSeasonsExtrasPatchouliCompat;
import io.github.lucaargolo.seasonsextras.payload.SendModulePressPacket;
import io.github.lucaargolo.seasonsextras.payload.SendTestedSeasonPacket;
import io.github.lucaargolo.seasonsextras.screenhandlers.AirConditioningScreenHandler;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FabricSeasonsExtras implements ModInitializer {

    public static final String MOD_ID = "seasonsextras";

    //Block Entities
    public static BlockEntityType<SeasonDetectorBlockEntity> SEASON_DETECTOR_TYPE = null;
    public static BlockEntityType<SeasonCalendarBlockEntity> SEASON_CALENDAR_TYPE = null;
    public static BlockEntityType<GreenhouseGlassBlockEntity> GREENHOUSE_GLASS_TYPE = null;
    public static BlockEntityType<AirConditioningBlockEntity> AIR_CONDITIONING_TYPE = null;

    //Blocks
    public static SeasonCalendarBlock SEASON_CALENDAR_BLOCK;
    public static GreenhouseGlassBlock[] GREENHOUSE_GLASS_BLOCKS = new GreenhouseGlassBlock[17];

    //Items
    public static Identifier SEASONAL_COMPENDIUM_ITEM_ID = ModIdentifier.of("seasonal_compendium");
    public static Item SEASON_CALENDAR_ITEM;
    
    //Screen Handlers
    public static ScreenHandlerType<AirConditioningScreenHandler> AIR_CONDITIONING_SCREEN_HANDLER;

    //Creative Tab
    private static final List<Pair<Predicate<Item>, Item>> creativeTabItems = new ArrayList<>();

    private static void addToTab(Predicate<Item> condition, Item item) {
        creativeTabItems.add(new Pair<>(condition, item));
    }
    
    private static void addToTab(Item item) {
        addToTab(i -> true, item);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, ModIdentifier.of("creative_tab"), FabricItemGroup.builder()
                .icon(() -> SEASON_CALENDAR_ITEM.getDefaultStack())
                .entries((displayContext, entries) -> {
                    creativeTabItems.forEach(pair -> {
                        Item item = pair.getRight();
                        if (pair.getLeft().test(item)) {
                            entries.add(item.getDefaultStack());
                        }
                    });
                })
                .displayName(Text.translatable("itemGroup.seasonsextras.creative_tab"))
                .build()
        );

        for (DyeColor value : DyeColor.values()) {
            GreenhouseGlassBlock greenhouseGlass = Registry.register(Registries.BLOCK, ModIdentifier.of(value.getName()+"_greenhouse_glass"), new GreenhouseGlassBlock(false, FabricBlockSettings.copyOf(Blocks.GREEN_STAINED_GLASS)));
            addToTab(i -> FabricSeasons.CONFIG.isSeasonMessingCrops(), Registry.register(Registries.ITEM, ModIdentifier.of(value.getName()+"_greenhouse_glass"), new GreenHouseGlassItem(greenhouseGlass, new Item.Settings())));
            GREENHOUSE_GLASS_BLOCKS[value.ordinal()] = greenhouseGlass;
        }
        GreenhouseGlassBlock tintedGreenhouseGlass = Registry.register(Registries.BLOCK, ModIdentifier.of("tinted_greenhouse_glass"), new GreenhouseGlassBlock(true, FabricBlockSettings.copyOf(Blocks.TINTED_GLASS)));
        addToTab(i -> FabricSeasons.CONFIG.isSeasonMessingCrops(), Registry.register(Registries.ITEM, ModIdentifier.of("tinted_greenhouse_glass"), new GreenHouseGlassItem(tintedGreenhouseGlass, new Item.Settings())));
        GREENHOUSE_GLASS_BLOCKS[16] = tintedGreenhouseGlass;
        GREENHOUSE_GLASS_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, ModIdentifier.of("greenhouse_glass"), FabricBlockEntityTypeBuilder.create(GreenhouseGlassBlockEntity::new, GREENHOUSE_GLASS_BLOCKS).build(null));

        AirConditioningBlock heaterBlock = Registry.register(Registries.BLOCK, ModIdentifier.of("heater"), new AirConditioningBlock(Conditioning.HEATER, FabricBlockSettings.copyOf(Blocks.COBBLESTONE).luminance(state -> state.get(AirConditioningBlock.LEVEL) * 5)));
        addToTab(i -> FabricSeasons.CONFIG.isSeasonMessingCrops(), Registry.register(Registries.ITEM, ModIdentifier.of("heater"), new AirConditioningItem(heaterBlock, new Item.Settings())));
        AirConditioningBlock chillerBlock = Registry.register(Registries.BLOCK, ModIdentifier.of("chiller"), new AirConditioningBlock(Conditioning.CHILLER, FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).luminance(state -> state.get(AirConditioningBlock.LEVEL) * 5)));
        addToTab(i -> FabricSeasons.CONFIG.isSeasonMessingCrops(), Registry.register(Registries.ITEM, ModIdentifier.of("chiller"), new AirConditioningItem(chillerBlock, new Item.Settings())));
        AIR_CONDITIONING_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, ModIdentifier.of("air_conditioning"), FabricBlockEntityTypeBuilder.create(AirConditioningBlockEntity::new, heaterBlock, chillerBlock).build(null));
        AIR_CONDITIONING_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER, ModIdentifier.of("air_conditioning_screen"), new ExtendedScreenHandlerType<> ((syncId, playerInventory, data) -> {
            return new AirConditioningScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerInventory.player.getWorld(), data.pos()), data.block());
        }, AirConditioningScreenHandler.Data.CODEC));

        SeasonDetectorBlock seasonDetector = Registry.register(Registries.BLOCK, ModIdentifier.of("season_detector"), new SeasonDetectorBlock(FabricBlockSettings.copyOf(Blocks.DAYLIGHT_DETECTOR)));
        SEASON_DETECTOR_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, ModIdentifier.of("season_detector"), FabricBlockEntityTypeBuilder.create(seasonDetector::createBlockEntity, seasonDetector).build(null));
        addToTab(Registry.register(Registries.ITEM, ModIdentifier.of("season_detector"), new SeasonDetectorItem(seasonDetector, new Item.Settings())));

        SEASON_CALENDAR_BLOCK = Registry.register(Registries.BLOCK, ModIdentifier.of("season_calendar"), new SeasonCalendarBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS)));
        SEASON_CALENDAR_TYPE = Registry.register(Registries.BLOCK_ENTITY_TYPE, ModIdentifier.of("season_calendar"), FabricBlockEntityTypeBuilder.create(SEASON_CALENDAR_BLOCK::createBlockEntity, SEASON_CALENDAR_BLOCK).build(null));
        SEASON_CALENDAR_ITEM = Registry.register(Registries.ITEM, ModIdentifier.of("season_calendar"), new SeasonCalendarItem(SEASON_CALENDAR_BLOCK, (new Item.Settings())));
        addToTab(SEASON_CALENDAR_ITEM);

        addToTab(i -> FabricLoader.getInstance().isModLoaded("patchouli"), Registry.register(Registries.ITEM, SEASONAL_COMPENDIUM_ITEM_ID, new SeasonalCompendiumItem(new Item.Settings())));
        addToTab(i -> FabricSeasons.CONFIG.isSeasonMessingCrops(), Registry.register(Registries.ITEM, ModIdentifier.of("crop_season_tester"), new CropSeasonTesterItem(new Item.Settings())));

        FabricSeasonsExtrasPatchouliCompat.onInitialize();

        PayloadTypeRegistry.playC2S().register(SendModulePressPacket.ID, SendModulePressPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SendModulePressPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                if(context.player().currentScreenHandler instanceof AirConditioningScreenHandler screenHandler) {
                    screenHandler.cycleButton(payload.button());
                }
            });
        });

        PayloadTypeRegistry.playS2C().register(SendTestedSeasonPacket.ID, SendTestedSeasonPacket.CODEC);

        ItemStorage.SIDED.registerForBlockEntity((entity, direction) -> {
            Storage<ItemVariant> inputStorage = InventoryStorage.of(entity.getInputInventory(), direction);
            Storage<ItemVariant> moduleStorage = FilteringStorage.extractOnlyOf(InventoryStorage.of(entity.getModuleInventory(), direction));
            return new CombinedStorage<>(List.of(inputStorage, moduleStorage));
        }, AIR_CONDITIONING_TYPE);

        ResourceConditions.register(SeasonMessingCropsCondition.TYPE);
    }

    public record SeasonMessingCropsCondition() implements ResourceCondition {

        public static final Identifier ID = ModIdentifier.of("is_season_messing_crops");

        public static final ResourceConditionType<SeasonMessingCropsCondition> TYPE = ResourceConditionType.create(
                ID, MapCodec.unit(new SeasonMessingCropsCondition())
        );

        @Override
        public ResourceConditionType<?> getType() {
            return TYPE;
        }

        @Override
        public boolean test(RegistryWrapper.@Nullable WrapperLookup registryLookup) {
            return FabricSeasons.CONFIG.isSeasonMessingCrops();
        }
    }

}
