package dev.sbwdronejammer.item;

import dev.sbwdronejammer.config.JammerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DroneJammerItem extends Item {
    public static final String ACTIVE_TAG = "JammerActive";

    public DroneJammerItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getItem() instanceof DroneJammerItem
                && stack.hasTag()
                && stack.getTag().getBoolean(ACTIVE_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean active = !isActive(stack);

        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean(ACTIVE_TAG, active);
            player.displayClientMessage(
                    Component.translatable(active
                            ? "message.sbwdronejammer.enabled"
                            : "message.sbwdronejammer.disabled"),
                    true
            );
            player.getCooldowns().addCooldown(this, 5);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sbwdronejammer.range", JammerConfig.RANGE.get())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(isActive(stack)
                        ? "tooltip.sbwdronejammer.active"
                        : "tooltip.sbwdronejammer.inactive")
                .withStyle(isActive(stack) ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.sbwdronejammer.toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
