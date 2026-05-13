package com.kynetio.hollowtanvil.menu;

import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import com.kynetio.hollowtanvil.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Display-only menu for the Hollow Anvil.
 * Exposes two data slots for automatic client sync:
 *   slot 0 → storedEssence
 *   slot 1 → bloodDemand
 */
public class HollowAnvilMenu extends AbstractContainerMenu {

    private final HollowAnvilBlockEntity blockEntity;
    private final ContainerData data;

    // ── Server-side constructor ───────────────────────────────────────────────

    public HollowAnvilMenu(int containerId, Inventory playerInventory,
                            HollowAnvilBlockEntity blockEntity) {
        super(ModMenuTypes.HOLLOW_ANVIL_MENU.get(), containerId);
        this.blockEntity = blockEntity;

        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.getStoredEssence();
                    case 1 -> blockEntity.getBloodDemand();
                    case 2 -> blockEntity.isAutoTrap() ? 1 : 0;
                    case 3 -> blockEntity.isPendingReward() ? 1 : 0;
                    case 4 -> blockEntity.getPendingChoice(0);
                    case 5 -> blockEntity.getPendingChoice(1);
                    case 6 -> blockEntity.getPendingChoice(2);
                    case 7 -> blockEntity.getDemandsMet();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                switch (index) {
                    case 0 -> blockEntity.setStoredEssence(value);
                    case 1 -> blockEntity.setBloodDemand(value);
                    case 2 -> blockEntity.setAutoTrap(value != 0);
                }
            }
            @Override public int getCount() { return 8; }
        };
        addDataSlots(this.data);
    }

    // ── Client-side factory constructor ──────────────────────────────────────

    public HollowAnvilMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }

    private static HollowAnvilBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof HollowAnvilBlockEntity altar) return altar;
        throw new IllegalStateException("No HollowAnvilBlockEntity at " + pos);
    }

    // ── Client-readable accessors ─────────────────────────────────────────────

    public int getStoredEssence() { return data.get(0); }
    public int getBloodDemand()   { return data.get(1); }
    public boolean isAutoTrap()   { return data.get(2) != 0; }
    public boolean isPendingReward() { return data.get(3) != 0; }
    public int getPendingChoice(int i) {
        return switch (i) {
            case 0 -> data.get(4);
            case 1 -> data.get(5);
            case 2 -> data.get(6);
            default -> -1;
        };
    }
    public int getDemandsMet() { return data.get(7); }
    public String getOwnerName() { return blockEntity.getOwnerName(); }
    public BlockPos getBlockEntityPos() { return blockEntity.getBlockPos(); }

    // ── Standard overrides ────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getBlockPos().distToCenterSqr(player.position()) < 64.0;
    }
}
