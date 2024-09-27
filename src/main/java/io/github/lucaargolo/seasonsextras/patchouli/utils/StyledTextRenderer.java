package io.github.lucaargolo.seasonsextras.patchouli.utils;

import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

import org.joml.Matrix4f;

public class StyledTextRenderer extends TextRenderer {

    private final Style style;

    public StyledTextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, boolean validateAdvance, Style style) {
        super(fontStorageAccessor, validateAdvance);
        this.style = style;
    }

    @Override
    public int draw(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, VertexConsumerProvider vertexConsumers, TextLayerType layerType, int backgroundColor, int light, boolean rightToLeft) {
        if(shadow)
            return super.draw(Text.literal(text).styled(s -> style), x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light);
        else
            return super.draw(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, rightToLeft);
    }
}
