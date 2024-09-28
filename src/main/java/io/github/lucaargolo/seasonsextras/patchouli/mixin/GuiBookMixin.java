package io.github.lucaargolo.seasonsextras.patchouli.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import vazkii.patchouli.client.book.gui.GuiBook;

import java.util.Iterator;

@Mixin(value = GuiBook.class)
public abstract class GuiBookMixin extends Screen {

    protected GuiBookMixin(Text title) {
        super(title);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(at = @At(value = "INVOKE", target = "Lvazkii/patchouli/client/book/gui/GuiBook;setDragging(Z)V"), method = "mouseClickedScaled", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void alsoSetFocused(double mouseX, double mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir, Iterator<Element> var6, Element listener) {
        if(listener instanceof TextFieldWidget) {
            this.setFocused(listener);
        }
    }

}
