package io.github.lucaargolo.seasonsextras.patchouli;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendBiomeMultiblocksPacket;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendMultiblocksPacket;
import io.github.lucaargolo.seasonsextras.patchouli.payload.SendValidBiomesPacket;
import io.github.lucaargolo.seasonsextras.payload.SendTestedSeasonPacket;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.TreeFeature;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class FabricSeasonsExtrasPatchouliCompat {

    private static final HashMap<Identifier, JsonObject> multiblockCache = new HashMap<>();

    public static void onInitialize() {
        PayloadTypeRegistry.playS2C().register(SendBiomeMultiblocksPacket.ID, SendBiomeMultiblocksPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SendMultiblocksPacket.ID, SendMultiblocksPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SendValidBiomesPacket.ID, SendValidBiomesPacket.CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sendValidBiomes(server, handler.player);
        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static void sendValidBiomes(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        boolean generateMultiblocks = player == null;
        if(generateMultiblocks) {
            multiblockCache.clear();
        }
        server.getWorlds().forEach(serverWorld -> {
            if(FabricSeasons.CONFIG.isValidInDimension(serverWorld.getRegistryKey())) {
                Set<RegistryEntry<Biome>> validBiomes = new HashSet<>();
                serverWorld.getChunkManager().getChunkGenerator().getBiomeSource().getBiomes().forEach(entry -> {
                    entry.getKey().ifPresent(key -> validBiomes.add(entry));
                });
                SendValidBiomesPacket packet = new SendValidBiomesPacket(serverWorld.getRegistryKey(), validBiomes.stream().map(r -> r.getKey().get().getValue()).collect(Collectors.toSet()));
                if(player != null) {
                    ServerPlayNetworking.send(player, packet);
                }else{
                    server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, packet));
                }
                sendBiomeMultiblocks(server, player, serverWorld, validBiomes);
            }
        });
        sendMultiblocks(server, player);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static void sendBiomeMultiblocks(MinecraftServer server, @Nullable ServerPlayerEntity player, ServerWorld serverWorld, Set<RegistryEntry<Biome>> validBiomes) {
        HashMap<Identifier, Set<Identifier>> biomeToMultiblocks = new HashMap<>();
        validBiomes.forEach(entry -> {
            Identifier biomeId = entry.getKey().get().getValue();
            List<ConfiguredFeature<?, ?>> validFeatures = entry.value().getGenerationSettings().getFeatures().stream()
                    .flatMap(RegistryEntryList::stream)
                    .map(RegistryEntry::value)
                    .flatMap(PlacedFeature::getDecoratedFeatures)
                    .filter((c) -> c.feature() instanceof TreeFeature)
                    .collect(ImmutableList.toImmutableList());

            for(ConfiguredFeature<?, ?> cf : validFeatures) {
                Identifier cfId = server.getRegistryManager().get(RegistryKeys.CONFIGURED_FEATURE).getId(cf);
                if(cfId != null) {
                    if (!multiblockCache.containsKey(cfId)) {
                        PatchouliMultiblockCreator creator = new PatchouliMultiblockCreator(serverWorld, Blocks.GRASS_BLOCK.getDefaultState(), Blocks.SHORT_GRASS.getDefaultState(), new BlockPos(-100, -100, -100), (c) -> {
                            cf.generate(c.getFakeWorld(), serverWorld.getChunkManager().getChunkGenerator(), Random.create(0L), new BlockPos(100, 100, 100));
                        });
                        Optional<JsonObject> optional = creator.getMultiblock((set) -> {
                            boolean foundLeave = false;
                            boolean foundLog = false;
                            Iterator<BlockState> iterator = set.iterator();
                            while(iterator.hasNext() && (!foundLeave || !foundLog)) {
                                BlockState state = iterator.next();
                                if(state.isIn(BlockTags.LEAVES)) {
                                    foundLeave = true;
                                }
                                if(state.isIn(BlockTags.LOGS)) {
                                    foundLog = true;
                                }
                            }
                            return foundLeave && foundLog;
                        });
                        optional.ifPresent((o) -> {
                            multiblockCache.put(cfId, o);
                            biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>()).add(cfId);
                        });
                    }else{
                        biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>()).add(cfId);
                    }
                }
            };
            Identifier empty = ModIdentifier.of("empty");
            if(multiblockCache.containsKey(empty)) {
                biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>(Collections.singleton(empty)));
            }else{
                PatchouliMultiblockCreator creator = new PatchouliMultiblockCreator(serverWorld, Blocks.SAND.getDefaultState(), Blocks.DEAD_BUSH.getDefaultState(), new BlockPos(0, 0, 0), (c) -> {});
                JsonObject emptyMultiblock = creator.getMultiblock((set) -> true).get();
                multiblockCache.put(empty, emptyMultiblock);
                biomeToMultiblocks.computeIfAbsent(biomeId, b -> new HashSet<>(Collections.singleton(empty)));
            }
        });
        SendBiomeMultiblocksPacket packet = new SendBiomeMultiblocksPacket(serverWorld.getRegistryKey(), biomeToMultiblocks);
        if(player != null) {
            ServerPlayNetworking.send(player, packet);
        }else{
            server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, packet));
        }

    }

    private static void sendMultiblocks(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        SendMultiblocksPacket packet = new SendMultiblocksPacket(multiblockCache);
        if(player != null) {
            ServerPlayNetworking.send(player, packet);
        }else{
            server.getPlayerManager().getPlayerList().forEach(p -> ServerPlayNetworking.send(p, packet));
        }
    }

}
