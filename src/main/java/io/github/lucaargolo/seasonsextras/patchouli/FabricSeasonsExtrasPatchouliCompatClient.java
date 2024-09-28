package io.github.lucaargolo.seasonsextras.patchouli;

import com.google.gson.*;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.resources.CropConfigs;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.client.FabricSeasonsExtrasClient;
import io.github.lucaargolo.seasonsextras.patchouli.mixin.GuiBookEntryAccessor;
import io.github.lucaargolo.seasonsextras.patchouli.page.*;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendBiomeMultiblocksPacket;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendMultiblocksPacket;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendValidBiomesPacket;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import io.github.lucaargolo.seasonsextras.utils.Tickable;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.client.book.text.BookTextParser;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricSeasonsExtrasPatchouliCompatClient {

    public static HashMap<Identifier, JsonObject> multiblocks = new HashMap<>();

    public static RegistryKey<Biome> multiblockBiomeOverride = null;
    public static Season multiblockSeasonOverride = null;

    public static final HashMap<RegistryKey<World>, Set<RegistryEntry<Biome>>> worldValidBiomes = new HashMap<>();
    public static final HashMap<RegistryKey<World>, HashMap<Identifier, Set<Identifier>>> worldBiomeMultiblocks = new HashMap<>();

    public static void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SendValidBiomesPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                worldValidBiomes.computeIfPresent(payload.worldKey(), (key, list) -> new LinkedHashSet<>());
                payload.validBiomes().stream().sorted(Comparator.comparing(Identifier::getPath)).forEach(biome -> {
                    Objects.requireNonNull(context.client().world).getRegistryManager().get(RegistryKeys.BIOME).getEntry(RegistryKey.of(RegistryKeys.BIOME, biome)).ifPresent(entry -> {
                        worldValidBiomes.computeIfAbsent(payload.worldKey(), (key) -> new LinkedHashSet<>()).add(entry);
                    });
                });
                reloadBook(context.client().world);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SendBiomeMultiblocksPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                worldBiomeMultiblocks.put(payload.worldKey(), payload.biomeMultiblocks());
                reloadBook(context.client().world);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(SendMultiblocksPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                multiblocks = payload.serverMultiblocks();
                reloadBook(context.client().world);
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.currentScreen == null) {
                multiblockBiomeOverride = null;
                multiblockSeasonOverride = null;
            }else{
                if(client.currentScreen instanceof GuiBookEntryAccessor bookEntry) {
                    if(bookEntry.getLeftPage() instanceof Tickable page) {
                        page.tick();
                    }
                    if(bookEntry.getRightPage() instanceof Tickable page) {
                        page.tick();
                    }
                }
            }
        });

        ClientBookRegistry.INSTANCE.pageTypes.put(ModIdentifier.of("biome_search"), PageBiomeSearch.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(ModIdentifier.of("seasonal_biome"), PageSeasonalBiome.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(ModIdentifier.of("biome_description"), PageBiomeDescription.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(ModIdentifier.of("crop_search"), PageCropSearch.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(ModIdentifier.of("multiple_crafting"), PageMultipleCrafting.class);


        BookTextParser.register((parameter, state) -> FabricSeasonsExtrasClient.prefersCelsius ? FabricSeasonsExtrasClient.minecraftToCelsius(parameter) : FabricSeasonsExtrasClient.minecraftToFahrenheit(parameter), "seasonsextrastemperature");
        BookTextParser.register((parameter, state) -> I18n.translate(parameter), "seasonsextrastranslate");

        PatchouliModifications.registerEntry(ModIdentifier.of("biomes"), ModIdentifier.of("seasonal_biomes"), (pages, index) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if(world != null) {
                RegistryKey<World> worldKey = world.getRegistryKey();
                AtomicInteger offset = new AtomicInteger(0);
                worldValidBiomes.getOrDefault(worldKey, new HashSet<>()).forEach(entry -> {
                    addSeasonalBiomePage(pages, index+offset.getAndAdd(2), worldKey, entry);
                });
            }
        });
        PatchouliModifications.registerEntry(ModIdentifier.of("crops"), ModIdentifier.of("seasonal_crops"), (pages, index) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            if(world != null) {
                AtomicInteger offset = new AtomicInteger(0);
                FabricSeasons.SEEDS_MAP.entrySet().stream().map(entry -> Map.entry(entry, Registries.BLOCK.getId(entry.getValue()))).sorted(Map.Entry.comparingByValue()).forEach((entry) -> {
                    Identifier seedId = Registries.ITEM.getId(entry.getKey().getKey());
                    Identifier cropId = Registries.BLOCK.getId(entry.getKey().getValue());
                    addSeasonalCropPage(pages, index+offset.getAndAdd(1), entry.getKey().getKey(), seedId, entry.getKey().getValue(), cropId);
                });
            }
        });
    }

    private static void reloadBook(World world) {
        Book book = BookRegistry.INSTANCE.books.get(FabricSeasonsExtras.SEASONAL_COMPENDIUM_ITEM_ID);
        if(book != null) {
            book.reloadContents(world, true);
            book.reloadLocks(false);
        }
    }


    private static void addSeasonalBiomePage(JsonArray pages, int index, RegistryKey<World> worldKey, RegistryEntry<Biome> entry) {

        Biome biome = entry.value();
        Identifier biomeId = entry.getKey().orElse(RegistryKey.of(RegistryKeys.BIOME, Identifier.ofVanilla("plains"))).getValue();

        String biomeName = biomeId.toTranslationKey("biome");
        Pair<Boolean, Float> springPair = FabricSeasons.getSeasonWeather(Season.SPRING, biomeId, biome.hasPrecipitation(),biome.getTemperature());
        Pair<Boolean, Float> summerPair = FabricSeasons.getSeasonWeather(Season.SUMMER, biomeId, biome.hasPrecipitation(),biome.getTemperature());
        Pair<Boolean, Float> fallPair = FabricSeasons.getSeasonWeather(Season.FALL, biomeId, biome.hasPrecipitation(),biome.getTemperature());
        Pair<Boolean, Float> winterPair = FabricSeasons.getSeasonWeather(Season.WINTER, biomeId, biome.hasPrecipitation(),biome.getTemperature());

        List<Season> rainSeasons = new ArrayList<>();
        List<Season> snowSeasons = new ArrayList<>();
        if(springPair.getLeft()) {
            if(springPair.getRight() < 0.15f) snowSeasons.add(Season.SPRING); else rainSeasons.add(Season.SPRING);
        }
        if(summerPair.getLeft()) {
            if(springPair.getRight() < 0.15f) snowSeasons.add(Season.SUMMER); else rainSeasons.add(Season.SUMMER);
        }
        if(fallPair.getLeft()) {
            if(springPair.getRight() < 0.15f) snowSeasons.add(Season.FALL); else rainSeasons.add(Season.FALL);
        }
        if(winterPair.getLeft()) {
            if(springPair.getRight() < 0.15f) snowSeasons.add(Season.WINTER); else rainSeasons.add(Season.WINTER);
        }

        String springTemperature = "$(seasonsextrastemperature:"+springPair.getRight()+")";
        String summerTemperature = "$(seasonsextrastemperature:"+summerPair.getRight()+")";
        String fallTemperature = "$(seasonsextrastemperature:"+fallPair.getRight()+")";
        String winterTemperature = "$(seasonsextrastemperature:"+winterPair.getRight()+")";

        StringBuilder rainInfo = new StringBuilder(rainSeasons.isEmpty() ? "$(seasonsextrastranslate:patchouli.seasonsextras.doesnotrain)" : "$(seasonsextrastranslate:patchouli.seasonsextras.rainsduring)"+" ");
        if(rainSeasons.size() == 4) {
            rainInfo.append("$(seasonsextrastranslate:patchouli.seasonsextras.all_seasons)");
        }else{
            for(int i = 0; i < rainSeasons.size(); i++) {
                Season season = rainSeasons.get(i);
                rainInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")");
                if(i == rainSeasons.size()-1) {
                    rainInfo.append("$(0).");
                }else if(i == rainSeasons.size()-2) {
                    rainInfo.append("$(0) ").append("$(seasonsextrastranslate:patchouli.seasonsextras.and)").append(" ");
                }else{
                    rainInfo.append("$(0), ");
                }
            }
        }

        StringBuilder snowInfo = new StringBuilder(snowSeasons.isEmpty() ? "$(seasonsextrastranslate:patchouli.seasonsextras.doesnotsnow)" : "$(seasonsextrastranslate:patchouli.seasonsextras.snowsduring)"+" ");
        if(snowSeasons.size() == 4) {
            snowInfo.append("$(seasonsextrastranslate:patchouli.seasonsextras.all_seasons)");
        }else {
            for (int i = 0; i < snowSeasons.size(); i++) {
                Season season = snowSeasons.get(i);
                snowInfo.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")");
                if (i == snowSeasons.size() - 1) {
                    snowInfo.append("$(0).");
                } else if (i == snowSeasons.size() - 2) {
                    snowInfo.append("$(0) ").append("$(seasonsextrastranslate:patchouli.seasonsextras.and)").append(" ");
                } else {
                    snowInfo.append("$(0), ");
                }
            }
        }

        String biomeInfo;
        if(springPair.getRight() <= 0.1) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisabiome)"+" $(3)"+"$(seasonsextrastranslate:patchouli.seasonsextras.frozenbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.frozendesc)";
        }else if(springPair.getRight() <= 0.3) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisabiome)"+" $(3)"+"$(seasonsextrastranslate:patchouli.seasonsextras.coldbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.colddesc)";
        }else if(springPair.getRight() <= 0.95) {
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisabiome)"+" $(2)"+"$(seasonsextrastranslate:patchouli.seasonsextras.temperatebiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.temperatedesc)";
        }else{
            biomeInfo = "$(seasonsextrastranslate:patchouli.seasonsextras.thisisabiome)"+" $(4)"+"$(seasonsextrastranslate:patchouli.seasonsextras.hotbiome)"+"$(0). "+"$(seasonsextrastranslate:patchouli.seasonsextras.hotdesc)";
        }

        JsonObject biomeFirstPage = new JsonObject();
        biomeFirstPage.add("type", new JsonPrimitive("seasonsextras:biome_description"));
        biomeFirstPage.add("title", new JsonPrimitive(biomeName));
        String biomeText = biomeInfo +
                "$(br2)" +
                "$(2)" + "$(seasonsextrastranslate:patchouli.seasonsextras.springtemp)" + ": $(0)" + springTemperature +
                "$(br)" +
                "$(6)" + "$(seasonsextrastranslate:patchouli.seasonsextras.summertemp)" + ": $(0)" + summerTemperature +
                "$(br)" +
                "$(c)" + "$(seasonsextrastranslate:patchouli.seasonsextras.falltemp)" + ": $(0)" + fallTemperature +
                "$(br)" +
                "$(3)" + "$(seasonsextrastranslate:patchouli.seasonsextras.wintertemp)" + ": $(0)" + winterTemperature +
                "$(br2)" +
                rainInfo +
                "$(br2)" +
                snowInfo;
        biomeFirstPage.add("text", new JsonPrimitive(biomeText));
        biomeFirstPage.add("anchor", new JsonPrimitive(biomeId.toString()));

        JsonObject biomeSecondPage = new JsonObject();
        biomeSecondPage.add("type", new JsonPrimitive("seasonsextras:seasonal_biome"));
        biomeSecondPage.add("name", new JsonPrimitive("Colors"));
        biomeSecondPage.add("enable_visualize", new JsonPrimitive(false));
        biomeSecondPage.add("biome_id", new JsonPrimitive(biomeId.toString()));

        if(worldBiomeMultiblocks.getOrDefault(worldKey, new HashMap<>()).containsKey(biomeId)) {
            JsonArray mbs = new JsonArray();
            worldBiomeMultiblocks.getOrDefault(worldKey, new HashMap<>()).get(biomeId).forEach(treeId -> {
                JsonObject multiblock = multiblocks.get(treeId);
                if (multiblock != null && multiblock.size() > 0) {
                    mbs.add(multiblock);
                }
            });
            biomeSecondPage.add("multiblocks", mbs);
        }

        JsonArray oldPages = pages.deepCopy();

        setOrAdd(pages, index, biomeFirstPage);
        setOrAdd(pages, index+1, biomeSecondPage);

        for(int i = index; i < oldPages.size(); i++) {
            setOrAdd(pages, index+2+(i-index), oldPages.get(i));
        }
    }

    private static void addSeasonalCropPage(JsonArray pages, int index, Item seed, Identifier seedId, Block crop, Identifier cropId) {

        float springMultiplier = CropConfigs.getSeasonCropMultiplier(cropId, Season.SPRING);
        float summerMultiplier = CropConfigs.getSeasonCropMultiplier(cropId, Season.SUMMER);
        float fallMultiplier = CropConfigs.getSeasonCropMultiplier(cropId, Season.FALL);
        float winterMultiplier = CropConfigs.getSeasonCropMultiplier(cropId, Season.WINTER);

        float maxMultiplier = Math.max(Math.max(springMultiplier, summerMultiplier), Math.max(fallMultiplier, winterMultiplier));
        List<Season> cropSeasons = new ArrayList<>();
        List<Season> alternateSeasons = new ArrayList<>();
        if(springMultiplier == maxMultiplier) cropSeasons.add(Season.SPRING); else alternateSeasons.add(Season.SPRING);
        if(summerMultiplier == maxMultiplier) cropSeasons.add(Season.SUMMER); else alternateSeasons.add(Season.SUMMER);
        if(fallMultiplier == maxMultiplier) cropSeasons.add(Season.FALL); else alternateSeasons.add(Season.FALL);
        if(winterMultiplier == maxMultiplier) cropSeasons.add(Season.WINTER); else alternateSeasons.add(Season.WINTER);

        StringBuilder cropText = new StringBuilder("$(seasonsextrastranslate:patchouli.seasonsextras.thisisacrop)"+" ");
        for(int i = 0; i < cropSeasons.size(); i++) {
            Season season = cropSeasons.get(i);
            cropText.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")");
            if(i == cropSeasons.size()-1) {
                cropText.append("$(0) $(seasonsextrastranslate:patchouli.seasonsextras.crop). ");
            }else if(i == cropSeasons.size()-2) {
                cropText.append("$(0) ").append("$(seasonsextrastranslate:patchouli.seasonsextras.and)").append(" ");
            }else{
                cropText.append("$(0), ");
            }
        }
        if(cropSeasons.size() == 1) {
            cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.growsfasterduringthis)$(br2)");
        }else{
            cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.growsfasterduringthese)$(br2)");
        }

        for (Season season : alternateSeasons) {
            float multiplier = CropConfigs.getSeasonCropMultiplier(cropId, season);
            if (multiplier == 0f) {
                cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.notgrowduring) ");
            } else if (multiplier < 1.0f) {
                cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.slowedgrowduring) ");
            } else if (multiplier == 1.0f) {
                cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.normalgrowduring) ");
            } else {
                cropText.append("$(seasonsextrastranslate:patchouli.seasonsextras.fastergrowduring) ");
            }
            cropText.append("$(").append(season.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(season.getTranslationKey()).append(")$(0)");
            cropText.append("$(br)");
        }
        cropText.append("$(br)");
        cropText.append("$(").append(Season.SPRING.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(Season.SPRING.getTranslationKey()).append(")$(0): ").append(springMultiplier).append("x $(seasonsextrastranslate:patchouli.seasonsextras.multiplier)$(br)");
        cropText.append("$(").append(Season.SUMMER.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(Season.SUMMER.getTranslationKey()).append(")$(0): ").append(summerMultiplier).append("x $(seasonsextrastranslate:patchouli.seasonsextras.multiplier)$(br)");
        cropText.append("$(").append(Season.FALL.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(Season.FALL.getTranslationKey()).append(")$(0): ").append(fallMultiplier).append("x $(seasonsextrastranslate:patchouli.seasonsextras.multiplier)$(br)");
        cropText.append("$(").append(Season.WINTER.getDarkFormatting().getCode()).append(")$(seasonsextrastranslate:").append(Season.WINTER.getTranslationKey()).append(")$(0): ").append(winterMultiplier).append("x $(seasonsextrastranslate:patchouli.seasonsextras.multiplier)$(br)");

        JsonObject biomeFirstPage = new JsonObject();
        biomeFirstPage.add("type", new JsonPrimitive("patchouli:spotlight"));
        biomeFirstPage.add("item", new JsonPrimitive(seedId.toString()));
        biomeFirstPage.add("title", new JsonPrimitive(crop.getTranslationKey()));
        biomeFirstPage.add("text", new JsonPrimitive(cropText.toString()));
        biomeFirstPage.add("anchor", new JsonPrimitive(cropId.toString()));

        JsonArray oldPages = pages.deepCopy();

        setOrAdd(pages, index, biomeFirstPage);

        for(int i = index; i < oldPages.size(); i++) {
            setOrAdd(pages, index+1+(i-index), oldPages.get(i));
        }
    }

    private static void setOrAdd(JsonArray array, int index, JsonElement element) {
        if(index == array.size()) {
            array.add(element);
        }else{
            array.set(index, element);
        }
    }

}
