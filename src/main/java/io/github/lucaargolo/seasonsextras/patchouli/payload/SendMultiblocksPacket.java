package io.github.lucaargolo.seasonsextras.patchouli.payload;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public record SendMultiblocksPacket(HashMap<Identifier, JsonObject> serverMultiblocks) implements CustomPayload {

    private static final PacketCodec<RegistryByteBuf, JsonObject> JSON_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, JsonElement::toString, (string) -> JsonParser.parseString(string).getAsJsonObject()
    );

    public static final Id<SendMultiblocksPacket> ID = new Id<>(ModIdentifier.of("send_multiblocks"));
    public static final PacketCodec<RegistryByteBuf, SendMultiblocksPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.map(HashMap::new, Identifier.PACKET_CODEC, JSON_CODEC), SendMultiblocksPacket::serverMultiblocks,
            SendMultiblocksPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

}
