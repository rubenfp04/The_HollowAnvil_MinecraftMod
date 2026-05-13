package com.kynetio.hollowtanvil.client.screen;

import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import com.kynetio.hollowtanvil.menu.HollowAnvilMenu;
import com.kynetio.hollowtanvil.network.CPacketChooseReward;
import com.kynetio.hollowtanvil.network.CPacketToggleAutoTrap;
import com.kynetio.hollowtanvil.reward.RewardType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class HollowAnvilScreen extends AbstractContainerScreen<HollowAnvilMenu> {

    private static final int BAR_X = 18;
    private static final int BAR_Y = 50;
    private static final int BAR_W = 140;
    private static final int BAR_H = 10;

    private Button autoTrapButton;
    private final Button[] rewardButtons = new Button[3];
    private boolean lastPendingState = false;

    public HollowAnvilScreen(HollowAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 125;
    }

    @Override
    protected void init() {
        super.init();
        rebuildAltarWidgets();
    }

    private void rebuildAltarWidgets() {
        clearWidgets();

        if (menu.isPendingReward()) {
            for (int i = 0; i < 3; i++) {
                final int idx = i;
                int choiceId = menu.getPendingChoice(i);
                RewardType type = RewardType.byId(choiceId);
                Component label = (type != null) ? type.label() : Component.literal("???");
                rewardButtons[i] = Button.builder(label, btn ->
                        ClientPacketDistributor.sendToServer(
                                new CPacketChooseReward(menu.getBlockEntityPos(), idx))
                ).bounds(leftPos + BAR_X, topPos + BAR_Y + i * 20, BAR_W, 18).build();
                addRenderableWidget(rewardButtons[i]);
            }
            autoTrapButton = null;
        } else {
            autoTrapButton = Button.builder(getAutoTrapLabel(), btn ->
                    ClientPacketDistributor.sendToServer(new CPacketToggleAutoTrap(menu.getBlockEntityPos()))
            ).bounds(leftPos + BAR_X, topPos + BAR_Y + BAR_H + 34, BAR_W, 16).build();
            addRenderableWidget(autoTrapButton);
            for (int i = 0; i < 3; i++) rewardButtons[i] = null;
        }

        lastPendingState = menu.isPendingReward();
    }

    private Component getAutoTrapLabel() {
        String key = menu.isAutoTrap()
                ? "gui.hollowtanvil.autotrap_on"
                : "gui.hollowtanvil.autotrap_off";
        return Component.translatable(key);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xD0100000);
        drawOutline(graphics, leftPos - 1, topPos - 1, imageWidth + 2, imageHeight + 2, 0xFF4A0000);
    }

    private static void drawOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.isPendingReward() != lastPendingState) {
            rebuildAltarWidgets();
        }

        int titleW = font.width(title);
        int centerX = imageWidth / 2;
        graphics.drawString(font, title, centerX - titleW / 2, titleLabelY, 0xFF4A0000, false);

        String owner = menu.getOwnerName();
        if (!owner.isEmpty()) {
            int dm = menu.getDemandsMet();
            int tier = dm >= 10 ? 4 : dm >= 6 ? 3 : dm >= 3 ? 2 : 1;
            String ownerLine = Component.translatable("gui.hollowtanvil.owner", owner).getString();
            String tierLine = Component.translatable("gui.hollowtanvil.tier", tier).getString();
            String combined = ownerLine + "  " + tierLine;
            int combinedW = font.width(combined);
            int ownerY = titleLabelY + 14;

            int boxX = centerX - combinedW / 2 - 4;
            int boxY2 = ownerY - 2;
            int boxW = combinedW + 8;
            int boxH2 = 13;
            graphics.fill(boxX, boxY2, boxX + boxW, boxY2 + boxH2, 0xC0200000);
            drawOutline(graphics, boxX, boxY2, boxW, boxH2, 0xFF660000);

            graphics.drawString(font, ownerLine, centerX - combinedW / 2, ownerY, 0xFFCC8800, false);
            int tierColor = switch (tier) { case 2 -> 0xFFFFE632; case 3 -> 0xFFFF8C1E; case 4 -> 0xFFA032FF; default -> 0xFF884400; };
            graphics.drawString(font, tierLine,
                    centerX - combinedW / 2 + font.width(ownerLine + "  "), ownerY, tierColor, false);
        }

        if (menu.isPendingReward()) {
            String header = Component.translatable("gui.hollowtanvil.choose_reward").getString();
            int hw = font.width(header);
            graphics.drawString(font, header,
                    BAR_X + (BAR_W - hw) / 2, BAR_Y - 14, 0xFFFF5500, false);
        } else {
            int essence = menu.getStoredEssence();
            int demand  = menu.getBloodDemand();
            if (demand <= 0) demand = 1;

            graphics.drawString(font,
                    Component.translatable("gui.hollowtanvil.blood_demanded"),
                    BAR_X, BAR_Y - 14, 0xFFCC3333, false);

            graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_W, BAR_Y + BAR_H, 0xFF1A0000);

            int filled = (int) ((float) essence / demand * BAR_W);
            filled = Math.min(filled, BAR_W);
            if (filled > 0) {
                graphics.fill(BAR_X, BAR_Y, BAR_X + filled, BAR_Y + BAR_H, 0xFF8B0000);
                graphics.fill(BAR_X + filled - 1, BAR_Y, BAR_X + filled, BAR_Y + BAR_H, 0xFFCC2222);
            }

            drawOutline(graphics, BAR_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, 0xFF4A0000);

            String numbers = essence + " / " + demand;
            int numW = font.width(numbers);
            graphics.drawString(font, numbers,
                    BAR_X + (BAR_W - numW) / 2, BAR_Y + 1, 0xFFFFAA55, false);

            String status = essence >= demand
                    ? Component.translatable("gui.hollowtanvil.reward_ready").getString()
                    : Component.translatable("gui.hollowtanvil.awaiting").getString();
            int statusColor = essence >= demand ? 0xFFFF5500 : 0xFF660000;
            graphics.drawString(font, status, BAR_X, BAR_Y + BAR_H + 6, statusColor, false);

            String maxLine = Component.translatable("gui.hollowtanvil.essence_stored").getString()
                    + ": " + essence + " / " + HollowAnvilBlockEntity.MAX_ESSENCE;
            graphics.drawString(font, maxLine, BAR_X, BAR_Y + BAR_H + 18, 0xFF550000, false);

            if (autoTrapButton != null) {
                autoTrapButton.setMessage(getAutoTrapLabel());
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
