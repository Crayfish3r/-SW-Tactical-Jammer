package dev.sbwdronejammer.client;

import com.mojang.math.Axis;
import dev.sbwdronejammer.SBWDroneJammer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRadarOverlay {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(SBWDroneJammer.MOD_ID, "textures/gui/radar_background.png");
    private static final int PANEL_SIZE = 150;
    private static final int MARGIN = 12;

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("jammer_radar", ClientRadarOverlay::render);
    }

    private static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                               int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientRadarState.isActive()) {
            return;
        }

        int left = screenWidth - PANEL_SIZE - MARGIN;
        int top = screenHeight - PANEL_SIZE - MARGIN;
        int right = left + PANEL_SIZE;
        int bottom = top + PANEL_SIZE;
        int centerX = left + PANEL_SIZE / 2;
        int centerY = top + PANEL_SIZE / 2;
        int radarRadius = 62;
        int configuredRange = ClientRadarState.serverRange();
        double scale = radarRadius / (double) configuredRange;

        graphics.blit(BACKGROUND, left, top, 0, 0, PANEL_SIZE, PANEL_SIZE, PANEL_SIZE, PANEL_SIZE);

        float sweepDegrees = (System.currentTimeMillis() % 3000L) / 3000.0F * 360.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(sweepDegrees));
        graphics.fill(0, -1, radarRadius, 1, 0xDD7CFF9B);
        graphics.pose().popPose();

        for (ClientRadarState.RadarContact contact : ClientRadarState.contacts()) {
            int dotX = centerX + (int) Math.round(contact.dx() * scale);
            int dotY = centerY + (int) Math.round(contact.dz() * scale);
            int color = contact.friendly() ? 0xFFFFB43C : 0xFFFF3030;
            graphics.fill(dotX - 3, dotY - 3, dotX + 4, dotY + 4, color);
        }

        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFF5FE8FF);

        Font font = minecraft.font;
        graphics.drawString(font, "JAMMER RADAR", left + 6, top + 5, 0xFF7CFF9B, false);
        String countText = "DRONES: " + ClientRadarState.contacts().size();
        graphics.drawString(font, countText, right - font.width(countText) - 6, top + 5, 0xFFE0FFE8, false);
        graphics.drawString(font, configuredRange + "m", left + 6, bottom - 14, 0xFF86A991, false);
    }

    private ClientRadarOverlay() {
    }
}
