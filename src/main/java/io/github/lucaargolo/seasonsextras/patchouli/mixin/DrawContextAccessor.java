package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {

    @Accessor
    MinecraftClient getClient();

    @Accessor @Mutable
    void setMatrices(MatrixStack matrices);

    @Accessor
    VertexConsumerProvider.Immediate getVertexConsumers();

}
