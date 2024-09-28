package io.github.lucaargolo.seasonsextras.mixed;

import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public interface TooltipContextMixed {

    @Nullable
    default World getWorld() {
        return null;
    };

}
