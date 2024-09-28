package io.github.lucaargolo.seasonsextras.utils;

import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import net.minecraft.util.Identifier;

public class ModIdentifier {

    public static Identifier of(String path) {
        return Identifier.of(FabricSeasonsExtras.MOD_ID, path);
    }

}
