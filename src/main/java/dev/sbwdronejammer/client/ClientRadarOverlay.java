package dev.sbwdronejammer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.sbwdronejammer.SBWDroneJammer;
import dev.sbwdronejammer.config.RadarClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = SBWDroneJammer.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRadarOverlay {
    private static final int FULL_WIDTH = 224;
    private static final int FULL_HEIGHT = 184;
    private static final int COMPACT_WIDTH = 126;
    private static final int COMPACT_HEIGHT = 28;
    private static final int RADAR_X = 82;
    private static final int RADAR_Y = 88;
    private static final int RADAR_RADIUS = 64;
    private static final int GREEN = 0xFF68F59A;
    private static final int MUTED_GREEN = 0xFF3B9960;
    private static final int CYAN = 0xFF63DEFF;
    private static final int HOSTILE = 0xFFFF4B4B;
    private static final int FRIENDLY = 0xFFFFB84D;
    private static final int TEXT = 0xFFE4FFEB;
    private static final int DIM_TEXT = 0xFF83A98E;
    private static final double SECTOR_DEGREES = 54.0;

    private static long lastFrameNanos;
    private static float expansion = 1.0F;

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("jammer_radar", ClientRadarOverlay::render);
    }

    private static void render(ForgeGui forgeGui, GuiGraphics graphics, float partialTick,
                               int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientRadarState.isActive()) {
            lastFrameNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        List<ClientRadarState.VisibleContact> contacts = ClientRadarState.visibleContacts(now);
        boolean compactEnabled = RadarClientConfig.COMPACT_WHEN_IDLE.get();
        float targetExpansion = !compactEnabled || !contacts.isEmpty() ? 1.0F : 0.0F;
        updateExpansion(now, targetExpansion);

        float scale = RadarClientConfig.HUD_SCALE.get().floatValue();
        if (expansion > 0.01F) {
            renderAtAnchor(graphics, screenWidth, screenHeight, FULL_WIDTH, FULL_HEIGHT, scale,
                    () -> renderFull(minecraft, graphics, contacts, now, smoothStep(expansion)));
        }
        if (compactEnabled && expansion < 0.99F) {
            renderAtAnchor(graphics, screenWidth, screenHeight, COMPACT_WIDTH, COMPACT_HEIGHT, scale,
                    () -> renderCompact(minecraft.font, graphics, 1.0F - smoothStep(expansion), now));
        }
    }

    private static void renderFull(Minecraft minecraft, GuiGraphics graphics,
                                   List<ClientRadarState.VisibleContact> contacts,
                                   long now, float transitionAlpha) {
        int backgroundAlpha = Math.round(255.0F * RadarClientConfig.BACKGROUND_OPACITY.get().floatValue()
                * transitionAlpha);
        renderBackground(graphics, backgroundAlpha, transitionAlpha);
        renderGrid(graphics, transitionAlpha);

        double sweepDegrees = (now / 1_000_000_000.0 * RadarClientConfig.SCAN_SPEED.get() * 360.0) % 360.0;
        renderScanSector(graphics, sweepDegrees, transitionAlpha);
        renderMarkers(minecraft, graphics, contacts, sweepDegrees, transitionAlpha);
        renderText(minecraft.font, graphics, contacts, transitionAlpha);
    }

    private static void renderBackground(GuiGraphics graphics, int backgroundAlpha, float alpha) {
        graphics.fill(0, 0, FULL_WIDTH, FULL_HEIGHT, backgroundAlpha << 24 | 0x07110D);
        graphics.fill(3, 3, FULL_WIDTH - 3, FULL_HEIGHT - 3, withAlpha(0x351A4029, alpha));
        graphics.fill(154, 24, 155, FULL_HEIGHT - 26, withAlpha(0x703B9960, alpha));
        graphics.fill(159, 28, FULL_WIDTH - 8, 29, withAlpha(0x503B9960, alpha));
        graphics.fill(159, 119, FULL_WIDTH - 8, 120, withAlpha(0x503B9960, alpha));
        graphics.fill(8, 162, FULL_WIDTH - 8, 163, withAlpha(0x603B9960, alpha));
        drawCornerBrackets(graphics, alpha);
    }

    private static void drawCornerBrackets(GuiGraphics graphics, float alpha) {
        int bright = withAlpha(GREEN, alpha);
        int dim = withAlpha(MUTED_GREEN, alpha * 0.75F);
        int length = 19;
        int inset = 2;
        graphics.fill(inset, inset, inset + length, inset + 2, bright);
        graphics.fill(inset, inset, inset + 2, inset + length, bright);
        graphics.fill(FULL_WIDTH - inset - length, inset, FULL_WIDTH - inset, inset + 2, bright);
        graphics.fill(FULL_WIDTH - inset - 2, inset, FULL_WIDTH - inset, inset + length, bright);
        graphics.fill(inset, FULL_HEIGHT - inset - 2, inset + length, FULL_HEIGHT - inset, dim);
        graphics.fill(inset, FULL_HEIGHT - inset - length, inset + 2, FULL_HEIGHT - inset, dim);
        graphics.fill(FULL_WIDTH - inset - length, FULL_HEIGHT - inset - 2,
                FULL_WIDTH - inset, FULL_HEIGHT - inset, dim);
        graphics.fill(FULL_WIDTH - inset - 2, FULL_HEIGHT - inset - length,
                FULL_WIDTH - inset, FULL_HEIGHT - inset, dim);
    }

    private static void renderGrid(GuiGraphics graphics, float alpha) {
        int grid = withAlpha(MUTED_GREEN, alpha * 0.22F);
        for (int offset = -48; offset <= 48; offset += 16) {
            int extent = (int) Math.sqrt(RADAR_RADIUS * RADAR_RADIUS - offset * offset);
            graphics.fill(RADAR_X + offset, RADAR_Y - extent,
                    RADAR_X + offset + 1, RADAR_Y + extent + 1, grid);
            graphics.fill(RADAR_X - extent, RADAR_Y + offset,
                    RADAR_X + extent + 1, RADAR_Y + offset + 1, grid);
        }
        drawCircle(graphics, RADAR_X, RADAR_Y, RADAR_RADIUS, withAlpha(GREEN, alpha * 0.72F), 72);
        drawCircle(graphics, RADAR_X, RADAR_Y, 48, withAlpha(MUTED_GREEN, alpha * 0.50F), 64);
        drawCircle(graphics, RADAR_X, RADAR_Y, 32, withAlpha(MUTED_GREEN, alpha * 0.58F), 48);
        drawCircle(graphics, RADAR_X, RADAR_Y, 16, withAlpha(MUTED_GREEN, alpha * 0.65F), 32);
        renderRangeTicks(graphics, alpha);
    }

    private static void renderRangeTicks(GuiGraphics graphics, float alpha) {
        int color = withAlpha(GREEN, alpha * 0.75F);
        Matrix4f matrix = graphics.pose().last().pose();
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double angle = Math.toRadians(degrees);
            float inner = degrees % 45 == 0 ? RADAR_RADIUS - 5.0F : RADAR_RADIUS - 3.0F;
            vertex(builder, matrix, RADAR_X + Math.cos(angle) * inner,
                    RADAR_Y + Math.sin(angle) * inner, color, 1.0F);
            vertex(builder, matrix, RADAR_X + Math.cos(angle) * RADAR_RADIUS,
                    RADAR_Y + Math.sin(angle) * RADAR_RADIUS, color, 1.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void renderScanSector(GuiGraphics graphics, double sweepDegrees, float alpha) {
        beginBlendedShape();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int slices = 18;
        for (int i = 0; i < slices; i++) {
            double a0 = Math.toRadians(sweepDegrees - SECTOR_DEGREES + SECTOR_DEGREES * i / slices);
            double a1 = Math.toRadians(sweepDegrees - SECTOR_DEGREES + SECTOR_DEGREES * (i + 1) / slices);
            float strength0 = (i + 1.0F) / slices;
            float strength1 = (i + 2.0F) / slices;
            vertex(builder, matrix, RADAR_X, RADAR_Y, GREEN, alpha * 0.03F);
            vertex(builder, matrix, RADAR_X + Math.cos(a0) * (RADAR_RADIUS - 1),
                    RADAR_Y + Math.sin(a0) * (RADAR_RADIUS - 1), GREEN, alpha * 0.02F * strength0);
            vertex(builder, matrix, RADAR_X + Math.cos(a1) * (RADAR_RADIUS - 1),
                    RADAR_Y + Math.sin(a1) * (RADAR_RADIUS - 1), GREEN, alpha * 0.16F * strength1);
        }
        BufferUploader.drawWithShader(builder.end());
        double leading = Math.toRadians(sweepDegrees);
        drawLine(matrix, RADAR_X, RADAR_Y,
                RADAR_X + (float) Math.cos(leading) * RADAR_RADIUS,
                RADAR_Y + (float) Math.sin(leading) * RADAR_RADIUS,
                withAlpha(GREEN, alpha * 0.9F));
        RenderSystem.disableBlend();
    }

    private static void renderMarkers(Minecraft minecraft, GuiGraphics graphics,
                                      List<ClientRadarState.VisibleContact> contacts,
                                      double sweepDegrees, float alpha) {
        int range = Math.max(1, ClientRadarState.serverRange());
        ClientRadarState.VisibleContact nearest = contacts.stream()
                .min(Comparator.comparingDouble(ClientRadarState.VisibleContact::distance))
                .orElse(null);
        for (ClientRadarState.VisibleContact contact : contacts) {
            double distance = contact.distance();
            double displayRadius = Math.min(RADAR_RADIUS - 6.0, distance * RADAR_RADIUS / range);
            double directionScale = distance > 0.0001 ? displayRadius / distance : 0.0;
            float x = RADAR_X + (float) (contact.dx() * directionScale);
            float y = RADAR_Y + (float) (contact.dz() * directionScale);
            double angle = Math.toDegrees(Math.atan2(contact.dz(), contact.dx()));
            double behindSweep = normalizeDegrees(sweepDegrees - angle);
            float scanPulse = behindSweep <= SECTOR_DEGREES
                    ? 1.0F - (float) (behindSweep / SECTOR_DEGREES)
                    : 0.0F;
            float markerAlpha = alpha * contact.alpha() * (0.72F + scanPulse * 0.28F);
            if (contact.friendly()) {
                drawFriendlyMarker(graphics, x, y, markerAlpha);
            } else {
                drawHostileMarker(graphics, x, y, markerAlpha);
            }
            if (scanPulse > 0.05F) {
                drawCircle(graphics, x, y, 5.0F + (1.0F - scanPulse) * 5.0F,
                        withAlpha(contact.friendly() ? FRIENDLY : HOSTILE,
                                markerAlpha * scanPulse * 0.75F), 20);
            }
            if (contact == nearest) {
                drawTargetBrackets(graphics, x, y, markerAlpha);
            }
        }
        drawPlayerMarker(minecraft, graphics, alpha);
    }

    private static void drawPlayerMarker(Minecraft minecraft, GuiGraphics graphics, float alpha) {
        graphics.pose().pushPose();
        graphics.pose().translate(RADAR_X, RADAR_Y, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(minecraft.player.getYRot() + 180.0F));
        Matrix4f matrix = graphics.pose().last().pose();
        fillTriangle(matrix, 0, -8, -5, 6, 0, 3, withAlpha(CYAN, alpha));
        fillTriangle(matrix, 0, -8, 0, 3, 5, 6, withAlpha(CYAN, alpha));
        graphics.pose().popPose();
        drawCircle(graphics, RADAR_X, RADAR_Y, 8, withAlpha(CYAN, alpha * 0.32F), 24);
    }

    private static void drawHostileMarker(GuiGraphics graphics, float x, float y, float alpha) {
        Matrix4f matrix = graphics.pose().last().pose();
        int color = withAlpha(HOSTILE, alpha);
        int core = withAlpha(0xFF471010, alpha);
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        addTriangle(builder, matrix, x, y - 5, x - 5, y, x, y + 5, color);
        addTriangle(builder, matrix, x, y - 5, x, y + 5, x + 5, y, color);
        addTriangle(builder, matrix, x, y - 3, x - 3, y, x, y + 3, core);
        addTriangle(builder, matrix, x, y - 3, x, y + 3, x + 3, y, core);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawFriendlyMarker(GuiGraphics graphics, float x, float y, float alpha) {
        fillCircle(graphics.pose().last().pose(), x, y, 4.5F, withAlpha(FRIENDLY, alpha), 16);
        fillCircle(graphics.pose().last().pose(), x, y, 2.0F, withAlpha(0xFF5C3A10, alpha), 12);
    }

    private static void drawTargetBrackets(GuiGraphics graphics, float x, float y, float alpha) {
        Matrix4f matrix = graphics.pose().last().pose();
        int color = withAlpha(TEXT, alpha * 0.9F);
        float r = 8.5F;
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        addLine(builder, matrix, x - r, y - r, x - r + 4, y - r, color);
        addLine(builder, matrix, x - r, y - r, x - r, y - r + 4, color);
        addLine(builder, matrix, x + r - 4, y - r, x + r, y - r, color);
        addLine(builder, matrix, x + r, y - r, x + r, y - r + 4, color);
        addLine(builder, matrix, x - r, y + r, x - r + 4, y + r, color);
        addLine(builder, matrix, x - r, y + r - 4, x - r, y + r, color);
        addLine(builder, matrix, x + r - 4, y + r, x + r, y + r, color);
        addLine(builder, matrix, x + r, y + r - 4, x + r, y + r, color);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void renderText(Font font, GuiGraphics graphics,
                                   List<ClientRadarState.VisibleContact> contacts, float alpha) {
        graphics.drawString(font, "JAMMER // RADAR", 9, 8, withAlpha(GREEN, alpha), false);
        String count = String.format("%02d TRACK", contacts.size());
        graphics.drawString(font, count, FULL_WIDTH - font.width(count) - 9, 8,
                withAlpha(TEXT, alpha), false);

        graphics.drawString(font, "N", RADAR_X - font.width("N") / 2, 15,
                withAlpha(GREEN, alpha), false);
        graphics.drawString(font, "S", RADAR_X - font.width("S") / 2, 153,
                withAlpha(GREEN, alpha), false);
        graphics.drawString(font, "W", 10, RADAR_Y - 4, withAlpha(GREEN, alpha), false);
        graphics.drawString(font, "E", 147, RADAR_Y - 4, withAlpha(GREEN, alpha), false);
        int range = ClientRadarState.serverRange();
        graphics.drawString(font, Integer.toString(Math.max(1, range / 4)), RADAR_X + 17, RADAR_Y - 10,
                withAlpha(DIM_TEXT, alpha * 0.72F), false);
        graphics.drawString(font, Integer.toString(Math.max(1, range / 2)), RADAR_X + 33, RADAR_Y - 10,
                withAlpha(DIM_TEXT, alpha * 0.72F), false);
        graphics.drawString(font, Integer.toString(Math.max(1, range * 3 / 4)), RADAR_X + 49, RADAR_Y - 10,
                withAlpha(DIM_TEXT, alpha * 0.72F), false);

        ClientRadarState.VisibleContact nearest = contacts.stream()
                .min(Comparator.comparingDouble(ClientRadarState.VisibleContact::distance))
                .orElse(null);
        if (RadarClientConfig.SHOW_TARGET_LABEL.get()) {
            graphics.drawString(font, "NEAREST", 162, 34, withAlpha(DIM_TEXT, alpha), false);
            if (nearest == null) {
                graphics.drawString(font, "NO TARGET", 162, 48, withAlpha(TEXT, alpha), false);
            } else {
                int affiliationColor = nearest.friendly() ? FRIENDLY : HOSTILE;
                graphics.drawString(font, "DRONE", 162, 48, withAlpha(TEXT, alpha), false);
                graphics.drawString(font, nearest.friendly() ? "FRIENDLY" : "HOSTILE", 162, 60,
                        withAlpha(affiliationColor, alpha * nearest.alpha()), false);
                String distance = Math.round(nearest.distance()) + " m";
                graphics.drawString(font, distance, 162, 72, withAlpha(TEXT, alpha), false);
            }
        }

        if (RadarClientConfig.SHOW_LEGEND.get()) {
            graphics.drawString(font, "IFF", 162, 125, withAlpha(DIM_TEXT, alpha), false);
            drawHostileMarker(graphics, 166, 140, alpha);
            graphics.drawString(font, "HOSTILE", 174, 136, withAlpha(HOSTILE, alpha), false);
            drawFriendlyMarker(graphics, 166, 153, alpha);
            graphics.drawString(font, "FRIEND", 174, 149, withAlpha(FRIENDLY, alpha), false);
        }

        String rangeText = "RANGE " + ClientRadarState.serverRange() + " m";
        graphics.drawString(font, rangeText, 10, 169, withAlpha(TEXT, alpha), false);
        graphics.drawString(font, "LINK ACTIVE", FULL_WIDTH - font.width("LINK ACTIVE") - 9, 169,
                withAlpha(GREEN, alpha), false);
    }

    private static void renderCompact(Font font, GuiGraphics graphics, float alpha, long now) {
        int backgroundAlpha = Math.round(255.0F * RadarClientConfig.BACKGROUND_OPACITY.get().floatValue() * alpha);
        graphics.fill(0, 0, COMPACT_WIDTH, COMPACT_HEIGHT, backgroundAlpha << 24 | 0x07110D);
        graphics.fill(1, 1, COMPACT_WIDTH - 1, 2, withAlpha(GREEN, alpha * 0.75F));
        graphics.fill(1, COMPACT_HEIGHT - 2, COMPACT_WIDTH - 1, COMPACT_HEIGHT - 1,
                withAlpha(MUTED_GREEN, alpha * 0.6F));
        float pulse = 0.65F + 0.35F * (float) Math.sin(now / 350_000_000.0);
        fillCircle(graphics.pose().last().pose(), 12, 14, 4, withAlpha(GREEN, alpha * pulse), 16);
        drawCircle(graphics, 12, 14, 7, withAlpha(GREEN, alpha * 0.35F), 20);
        graphics.drawString(font, "RADAR ACTIVE", 25, 7, withAlpha(TEXT, alpha), false);
        graphics.drawString(font, "CLEAR", 25, 17, withAlpha(DIM_TEXT, alpha), false);
        String range = ClientRadarState.serverRange() + "m";
        graphics.drawString(font, range, COMPACT_WIDTH - font.width(range) - 7, 12,
                withAlpha(GREEN, alpha), false);
    }

    private static void renderAtAnchor(GuiGraphics graphics, int screenWidth, int screenHeight,
                                       int width, int height, float scale, Runnable renderer) {
        int offsetX = RadarClientConfig.OFFSET_X.get();
        int offsetY = RadarClientConfig.OFFSET_Y.get();
        RadarClientConfig.ScreenPosition position = RadarClientConfig.POSITION.get();
        float x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width * scale - offsetX;
        };
        float y = switch (position) {
            case TOP_LEFT, TOP_RIGHT -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - height * scale - offsetY;
        };
        graphics.pose().pushPose();
        graphics.pose().translate(Math.round(x), Math.round(y), 0);
        graphics.pose().scale(scale, scale, 1.0F);
        renderer.run();
        graphics.pose().popPose();
    }

    private static void updateExpansion(long now, float target) {
        if (lastFrameNanos == 0L) {
            expansion = target;
        } else {
            float deltaSeconds = Math.min(0.1F, (now - lastFrameNanos) / 1_000_000_000.0F);
            float speed = target > expansion ? 5.5F : 3.5F;
            expansion += (target - expansion) * Math.min(1.0F, deltaSeconds * speed);
            if (Math.abs(target - expansion) < 0.005F) {
                expansion = target;
            }
        }
        lastFrameNanos = now;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, multiplier)));
        return color & 0x00FFFFFF | alpha << 24;
    }

    private static void beginBlendedShape() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void drawCircle(GuiGraphics graphics, float centerX, float centerY,
                                   float radius, int color, int segments) {
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = graphics.pose().last().pose();
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            vertex(builder, matrix, centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius, color, 1.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void fillCircle(Matrix4f matrix, float centerX, float centerY,
                                   float radius, int color, int segments) {
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vertex(builder, matrix, centerX, centerY, color, 1.0F);
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0 * i / segments;
            vertex(builder, matrix, centerX + Math.cos(angle) * radius,
                    centerY + Math.sin(angle) * radius, color, 1.0F);
        }
        BufferUploader.drawWithShader(builder.end());
    }

    private static void drawLine(Matrix4f matrix, float x1, float y1, float x2, float y2, int color) {
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        vertex(builder, matrix, x1, y1, color, 1.0F);
        vertex(builder, matrix, x2, y2, color, 1.0F);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void fillTriangle(Matrix4f matrix, float x1, float y1, float x2, float y2,
                                     float x3, float y3, int color) {
        beginBlendedShape();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        vertex(builder, matrix, x1, y1, color, 1.0F);
        vertex(builder, matrix, x2, y2, color, 1.0F);
        vertex(builder, matrix, x3, y3, color, 1.0F);
        BufferUploader.drawWithShader(builder.end());
    }

    private static void addTriangle(BufferBuilder builder, Matrix4f matrix,
                                    float x1, float y1, float x2, float y2,
                                    float x3, float y3, int color) {
        vertex(builder, matrix, x1, y1, color, 1.0F);
        vertex(builder, matrix, x2, y2, color, 1.0F);
        vertex(builder, matrix, x3, y3, color, 1.0F);
    }

    private static void addLine(BufferBuilder builder, Matrix4f matrix,
                                float x1, float y1, float x2, float y2, int color) {
        vertex(builder, matrix, x1, y1, color, 1.0F);
        vertex(builder, matrix, x2, y2, color, 1.0F);
    }

    private static void vertex(BufferBuilder builder, Matrix4f matrix, double x, double y,
                               int color, float alphaMultiplier) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * alphaMultiplier);
        builder.vertex(matrix, (float) x, (float) y, 0.0F)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha)
                .endVertex();
    }

    private ClientRadarOverlay() {
    }
}
