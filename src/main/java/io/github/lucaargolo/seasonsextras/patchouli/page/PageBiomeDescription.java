package io.github.lucaargolo.seasonsextras.patchouli.page;

import io.github.lucaargolo.seasonsextras.client.FabricSeasonsExtrasClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import vazkii.patchouli.client.book.gui.BookTextRenderer;
import vazkii.patchouli.client.book.gui.GuiBookEntry;
import vazkii.patchouli.client.book.page.PageText;

public class PageBiomeDescription extends PageText {

    private transient BookTextRenderer textRender;

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float pticks) {
        super.render(graphics, mouseX, mouseY, pticks);
        textRender.render(graphics, mouseX, mouseY, pticks);
        int mx = mouseX-parent.bookLeft;
        int my = mouseY-parent.bookTop;
        graphics.drawTexture(parent.getBookTexture(), -28, 0, 352f+27f, FabricSeasonsExtrasClient.prefersCelsius ? 56f : 66f, 13, 10, 512, 256);
        if(mx > -28 && mx < -28+13 && my > 0 && my < 10) {
            graphics.drawTexture(parent.getBookTexture(), -28, 0, 352f+14f, FabricSeasonsExtrasClient.prefersCelsius ? 56f : 66f, 13, 10, 512, 256);
            Text tooltip = FabricSeasonsExtrasClient.prefersCelsius ? Text.translatable("patchouli.seasonsextras.changetofahrenheit") : Text.translatable("patchouli.seasonsextras.changetocelsius");
            graphics.drawTooltip(fontRenderer, tooltip, mx, my);
        }
    }

    @Override
    public void onDisplayed(GuiBookEntry parent, int left, int top) {
        super.onDisplayed(parent, left, top);
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, 12);
        updateText();
    }

    public void updateText() {
        textRender = new BookTextRenderer(parent, text.as(Text.class), 0, getTextHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int mx = (int) mouseX-parent.bookLeft;
        int my = (int) mouseY-parent.bookTop;
        if(mx > -28 && mx < -28+13 && my > 0 && my < 10) {
            FabricSeasonsExtrasClient.prefersCelsius = !FabricSeasonsExtrasClient.prefersCelsius;
            updateText();
            return true;
        }
        return textRender.click(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean shouldRenderText() {
        return false;
    }

}