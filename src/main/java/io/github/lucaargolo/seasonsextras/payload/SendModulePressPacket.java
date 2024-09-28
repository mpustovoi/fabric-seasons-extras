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

public record SendModulePressPacket(int button) implements CustomPayload {

    public static final Id<SendModulePressPacket> ID = new Id<>(ModIdentifier.of("send_module_press"));
    public static final PacketCodec<RegistryByteBuf, SendModulePressPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SendModulePressPacket::button,
            SendModulePressPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }


}
