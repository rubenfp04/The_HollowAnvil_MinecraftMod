package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(HollowAnvilMod.MODID);

    public static final DeferredItem<BlockItem> HOLLOW_ANVIL =
            ITEMS.registerItem(
                    "hollow_anvil",
                    props -> new BlockItem(ModBlocks.HOLLOW_ANVIL.get(), props),
                    new Item.Properties().stacksTo(1).fireResistant()
            );

    private ModItems() {}
}
