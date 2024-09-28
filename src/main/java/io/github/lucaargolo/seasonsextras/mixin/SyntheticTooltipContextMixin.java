package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.mixed.TooltipContextMixed;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/item/Item$TooltipContext$2")
public abstract class SyntheticTooltipContextMixin implements Item.TooltipContext, TooltipContextMixed {

    @Unique
    private World capturedWorld;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void afterInit(World world, CallbackInfo ci) {
        capturedWorld = world;
    }

    @Override
    public @Nullable World getWorld() {
        return capturedWorld;
    }

}
