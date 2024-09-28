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

import java.util.HashSet;
import java.util.Set;

public record SendValidBiomesPacket(RegistryKey<World> worldKey, Set<Identifier> validBiomes) implements CustomPayload {

    public static final CustomPayload.Id<SendValidBiomesPacket> ID = new CustomPayload.Id<>(ModIdentifier.of("send_valid_biomes"));
    public static final PacketCodec<RegistryByteBuf, SendValidBiomesPacket> CODEC = PacketCodec.tuple(
            RegistryKey.createPacketCodec(RegistryKeys.WORLD), SendValidBiomesPacket::worldKey,
            PacketCodecs.collection(HashSet::new, Identifier.PACKET_CODEC), SendValidBiomesPacket::validBiomes,
            SendValidBiomesPacket::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

}
