package io.github.lucaargolo.seasonsextras.patchouli.payload;

import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public record SendBiomeMultiblocksPacket(RegistryKey<World> worldKey, HashMap<Identifier, Set<Identifier>> biomeMultiblocks) implements CustomPayload {

    public static final CustomPayload.Id<SendBiomeMultiblocksPacket> ID = new CustomPayload.Id<>(ModIdentifier.of("send_biome_multiblocks"));
    public static final PacketCodec<RegistryByteBuf, SendBiomeMultiblocksPacket> CODEC = PacketCodec.tuple(
            RegistryKey.createPacketCodec(RegistryKeys.WORLD), SendBiomeMultiblocksPacket::worldKey,
            PacketCodecs.map(HashMap::new, Identifier.PACKET_CODEC, PacketCodecs.collection(HashSet::new, Identifier.PACKET_CODEC)), SendBiomeMultiblocksPacket::biomeMultiblocks,
            SendBiomeMultiblocksPacket::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

}
