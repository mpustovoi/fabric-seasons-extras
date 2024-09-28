package io.github.lucaargolo.seasonsextras.item;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.FertilizableUtil;
import io.github.lucaargolo.seasons.utils.GreenhouseCache;
import io.github.lucaargolo.seasons.utils.Season;
import io.github.lucaargolo.seasonsextras.FabricSeasonsExtras;
import io.github.lucaargolo.seasonsextras.payload.SendTestedSeasonPacket;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CropSeasonTesterItem extends Item {

    public CropSeasonTesterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context.getWorld() instanceof ServerWorld serverWorld) {
            BlockPos blockPos = context.getBlockPos();
            BlockState blockState = serverWorld.getBlockState(blockPos);
            Season blockSeason = GreenhouseCache.test(serverWorld, blockPos);
            Season upperSeason = GreenhouseCache.test(serverWorld, blockPos.up());
            PlayerEntity player = context.getPlayer();
            if(player instanceof ServerPlayerEntity serverPlayer) {
                List<Text> tooltip = new ArrayList<>();
                if(!FabricSeasons.CONFIG.isSeasonMessingCrops() || (FabricSeasons.CONFIG.doCropsGrowsNormallyUnderground() && serverWorld.getLightLevel(LightType.SKY, blockPos.up()) == 0)) {
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_disabled_1"));
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_disabled_2"));
                } else if(!player.isSneaking() && FabricSeasons.SEEDS_MAP.containsValue(blockState.getBlock())) {
                    float multiplier = FertilizableUtil.getMultiplier(serverWorld, blockPos, blockState);
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_crop_1", multiplier));
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_crop_2", Text.translatable(blockSeason.getTranslationKey()).formatted(blockSeason.getFormatting())));
                }else {
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_enabled_1"));
                    tooltip.add(Text.translatable("tooltip.seasonsextras.tester_result_enabled_2", Text.translatable(upperSeason.getTranslationKey()).formatted(upperSeason.getFormatting())));
                }
                ServerPlayNetworking.send(serverPlayer, new SendTestedSeasonPacket(blockPos, tooltip));
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.seasonsextras.crop_tester_1").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        tooltip.add(Text.translatable("tooltip.seasonsextras.crop_tester_2").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        if(!FabricSeasons.CONFIG.isSeasonMessingCrops()) {
            tooltip.add(Text.translatable("tooltip.seasonsextras.not_enabled").formatted(Formatting.RED, Formatting.ITALIC));
        }
    }

}
