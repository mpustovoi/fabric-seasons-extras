package io.github.lucaargolo.seasonsextras.patchouli.page;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.patchouli.FabricSeasonsExtrasPatchouliCompatClient;
import io.github.lucaargolo.seasonsextras.patchouli.mixin.PageMultiblockAccessor;
import io.github.lucaargolo.seasonsextras.utils.Tickable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.page.PageMultiblock;
import vazkii.patchouli.common.multiblock.AbstractMultiblock;
import vazkii.patchouli.common.multiblock.SerializedMultiblock;

import java.util.ArrayList;
import java.util.List;

public class PageSeasonalBiome extends PageMultiblock implements Tickable {

    @SerializedName("biome_id") Identifier biomeId;
    @SerializedName("multiblocks") SerializedMultiblock[] serializedMultiblocks;

    private final transient List<AbstractMultiblock> multiblockObjs = new ArrayList<>();

    private transient Season selectedSeason = Season.SPRING;
    private transient int age = 0;
    private transient int index = 0;


    @Override
    public void build(World level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        PageMultiblockAccessor accessor = (PageMultiblockAccessor) this;
        if(serializedMultiblocks != null && serializedMultiblocks.length > 0) {
            accessor.setSerializedMultiblock(serializedMultiblocks[0]);
            for (SerializedMultiblock serializedMultiblock : serializedMultiblocks) {
                multiblockObjs.add(serializedMultiblock.toMultiblock());
            }
            super.build(level, entry, builder, pageNum);
        }
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float pticks) {
        if(serializedMultiblocks != null) {
            PageMultiblockAccessor accessor = (PageMultiblockAccessor) this;
            int newIndex = (age/40) % serializedMultiblocks.length;
            if(newIndex != index) {
                index = newIndex;
                accessor.setMultiblockObj(multiblockObjs.get(index));
            }
        }

        FabricSeasonsExtrasPatchouliCompatClient.multiblockSeasonOverride = selectedSeason;
        if(biomeId != null) {
            FabricSeasonsExtrasPatchouliCompatClient.multiblockBiomeOverride = RegistryKey.of(RegistryKeys.BIOME, biomeId);
        }
        super.render(graphics, mouseX, mouseY, pticks);
        FabricSeasonsExtrasPatchouliCompatClient.multiblockBiomeOverride = null;
        FabricSeasonsExtrasPatchouliCompatClient.multiblockSeasonOverride = null;
        RenderSystem.setShaderTexture(0, parent.getBookTexture());
        graphics.drawTexture(parent.getBookTexture(), 6, 115, 23, 23, 352, selectedSeason == Season.SPRING ? 0 : 28, 28, 28, 512, 256);
        graphics.drawTexture(parent.getBookTexture(), 33, 115, 23, 23, 380, selectedSeason == Season.SUMMER ? 0 : 28, 28, 28, 512, 256);
        graphics.drawTexture(parent.getBookTexture(), 60, 115, 23, 23, 408, selectedSeason == Season.FALL ? 0 : 28, 28, 28, 512, 256);
        graphics.drawTexture(parent.getBookTexture(), 87, 115, 23, 23, 436, selectedSeason == Season.WINTER ? 0 : 28, 28, 28, 512, 256);
        int mx = mouseX-parent.bookLeft;
        int my = mouseY-parent.bookTop;
        if(mx > 6 && mx < 6+23 && my > 115 && my < 115+23) {
            graphics.drawTooltip(fontRenderer, Text.translatable(Season.SPRING.getTranslationKey()).formatted(Season.SPRING.getFormatting()), mx, my);
        }else if(mx > 33 && mx < 33+23 && my > 115 && my < 115+23) {
            graphics.drawTooltip(fontRenderer, Text.translatable(Season.SUMMER.getTranslationKey()).formatted(Season.SUMMER.getFormatting()), mx, my);
        }else if(mx > 60 && mx < 60+23 && my > 115 && my < 115+23) {
            graphics.drawTooltip(fontRenderer, Text.translatable(Season.FALL.getTranslationKey()).formatted(Season.FALL.getFormatting()), mx, my);
        }else if(mx > 87 && mx < 87+23 && my > 115 && my < 115+23) {
            graphics.drawTooltip(fontRenderer, Text.translatable(Season.WINTER.getTranslationKey()).formatted(Season.WINTER.getFormatting()), mx, my);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        MinecraftClient client = MinecraftClient.getInstance();
        int mx = (int) mouseX-parent.bookLeft;
        int my = (int) mouseY-parent.bookTop;
        if(mx > 6 && mx < 6+23 && my > 115 && my < 115+23) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectedSeason = Season.SPRING;
            return true;
        }else if(mx > 33 && mx < 33+23 && my > 115 && my < 115+23) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectedSeason = Season.SUMMER;
            return true;
        }else if(mx > 60 && mx < 60+23 && my > 115 && my < 115+23) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectedSeason = Season.FALL;
            return true;
        }else if(mx > 87 && mx < 87+23 && my > 115 && my < 115+23) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectedSeason = Season.WINTER;
            return true;
        }else return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void tick() {
        age++;
    }


}