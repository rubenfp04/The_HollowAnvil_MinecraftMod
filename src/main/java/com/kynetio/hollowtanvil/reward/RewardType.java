package com.kynetio.hollowtanvil.reward;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Pool of possible reward options offered when the altar's demand is met.
 * The player picks one of three randomly-selected entries from this pool.
 */
public enum RewardType {
    GOLDEN_APPLE      ("reward.hollowtanvil.golden_apple",   new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),
    HOLLOW_SWORD      ("reward.hollowtanvil.hollow_sword",   new ItemStack(Items.NETHERITE_SWORD)),
    HOLLOW_TOTEM      ("reward.hollowtanvil.hollow_totem",   new ItemStack(Items.TOTEM_OF_UNDYING)),
    BLOOD_INGOTS      ("reward.hollowtanvil.blood_ingots",   new ItemStack(Items.NETHERITE_INGOT)),
    POWER_ENDERMAN    ("reward.hollowtanvil.power_enderman", new ItemStack(Items.ENDER_PEARL)),
    POWER_GHAST       ("reward.hollowtanvil.power_ghast",    new ItemStack(Items.FIRE_CHARGE)),
    POWER_WITHER      ("reward.hollowtanvil.power_wither",   new ItemStack(Items.WITHER_SKELETON_SKULL)),
    POWER_CREEPER     ("reward.hollowtanvil.power_creeper",  new ItemStack(Items.TNT)),
    POWER_BLAZE       ("reward.hollowtanvil.power_blaze",    new ItemStack(Items.BLAZE_POWDER)),
    POWER_SPIDER      ("reward.hollowtanvil.power_spider",   new ItemStack(Items.COBWEB));

    private static final RewardType[] VALUES = values();

    private final String labelKey;
    private final ItemStack iconStack;

    RewardType(String labelKey, ItemStack iconStack) {
        this.labelKey  = labelKey;
        this.iconStack = iconStack;
    }

    public Component label()      { return Component.translatable(labelKey); }
    public ItemStack iconStack()  { return iconStack; }
    public boolean isPower()      { return ordinal() >= POWER_ENDERMAN.ordinal(); }

    public static RewardType byId(int id) {
        if (id < 0 || id >= VALUES.length) return null;
        return VALUES[id];
    }

    public static int count() { return VALUES.length; }
}
