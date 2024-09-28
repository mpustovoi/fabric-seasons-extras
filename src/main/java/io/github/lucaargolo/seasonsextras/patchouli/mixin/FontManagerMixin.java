package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import io.github.lucaargolo.seasonsextras.patchouli.mixed.FontManagerMixed;
import io.github.lucaargolo.seasonsextras.patchouli.utils.StyledTextRenderer;
import net.minecraft.client.font.FontManager;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FontManager.class)
public abstract class FontManagerMixin implements FontManagerMixed {

    @Shadow protected abstract FontStorage getStorage(Identifier id);

    @Override
    public TextRenderer createStyledTextRenderer(Style style) {
        return new StyledTextRenderer(this::getStorage, false, style);
    }


}
