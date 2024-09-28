package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.mixed.TooltipContextMixed;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class SeasonCalendarItem extends BlockItem {


    public SeasonCalendarItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        World world = ((TooltipContextMixed) context).getWorld();
        if(world != null) {
            appendCalendarTooltip(world, tooltip);
        }
    }

    public static void appendCalendarTooltip(World world, List<Text> tooltip) {
        Season season = FabricSeasons.getCurrentSeason(world);
        tooltip.add(Text.translatable("tooltip.seasonsextras.calendar_info_1").formatted(season.getFormatting()).append(Text.translatable(season.getTranslationKey()).formatted(season.getFormatting())).formatted(Formatting.UNDERLINE));
        if(!FabricSeasons.CONFIG.isSeasonLocked() && !FabricSeasons.CONFIG.isSeasonTiedWithSystemTime())
            tooltip.add(Text.literal(Long.toString(FabricSeasons.getTimeToNextSeason(world)/24000L)).append(Text.translatable("tooltip.seasonsextras.calendar_info_2").formatted(Formatting.GRAY).append(Text.translatable("tooltip.seasons."+season.getNext().name().toLowerCase(Locale.ROOT)).formatted(season.getNext().getFormatting()))));
    }

}
