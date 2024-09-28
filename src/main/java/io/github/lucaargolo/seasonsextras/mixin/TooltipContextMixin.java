package io.github.lucaargolo.seasonsextras.mixin;

import io.github.lucaargolo.seasonsextras.mixed.TooltipContextMixed;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.TooltipContext.class)
public interface TooltipContextMixin extends TooltipContextMixed {
}
