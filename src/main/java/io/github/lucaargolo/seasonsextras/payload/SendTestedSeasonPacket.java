package io.github.lucaargolo.seasonsextras.payload;

import io.github.lucaargolo.seasonsextras.utils.ModIdentifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record SendTestedSeasonPacket(BlockPos testedPos, List<Text> tooltip) implements CustomPayload {

    public static final CustomPayload.Id<SendTestedSeasonPacket> ID = new CustomPayload.Id<>(ModIdentifier.of("send_tested_season"));
    public static final PacketCodec<RegistryByteBuf, SendTestedSeasonPacket> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, SendTestedSeasonPacket::testedPos,
            PacketCodecs.collection(ArrayList::new, TextCodecs.PACKET_CODEC), SendTestedSeasonPacket::tooltip,
            SendTestedSeasonPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


}
