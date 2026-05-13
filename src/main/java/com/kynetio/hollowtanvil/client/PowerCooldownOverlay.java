package com.kynetio.hollowtanvil.client;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.GuiLayer;

public final class PowerCooldownOverlay implements GuiLayer {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "power_cooldown");

    private static int remainingTicks = 0;
    private static int totalTicks = 0;

    public static void setCooldown(int remaining, int total) {
        remainingTicks = remaining;
        totalTicks = total;
    }

    public static void clientTick() {
        if (remainingTicks > 0) remainingTicks--;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        if (remainingTicks <= 0 || totalTicks <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int seconds = (remainingTicks + 19) / 20;
        String text = "§c⏳ " + seconds + "s";

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        int textW = mc.font.width(text);
        int x = screenW / 2 + 95;
        int y = screenH - 47;

        float progress = (float) remainingTicks / totalTicks;
        int barW = 40;
        int barH = 3;
        int barX = x;
        int barY = y + 11;

        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x80000000);
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A0000);
        int filledW = (int) (barW * progress);
        if (filledW > 0) {
            graphics.fill(barX, barY, barX + filledW, barY + barH, 0xFF8B0000);
        }

        graphics.drawString(mc.font, text, x, y, 0xFFFF4444, true);
    }
}
