package com.kynetio.hollowtanvil;

import com.kynetio.hollowtanvil.registry.ModBlockEntities;
import com.kynetio.hollowtanvil.registry.ModBlocks;
import com.kynetio.hollowtanvil.registry.ModCreativeTabs;
import com.kynetio.hollowtanvil.registry.ModEffects;
import com.kynetio.hollowtanvil.registry.ModItems;
import com.kynetio.hollowtanvil.registry.ModMenuTypes;
import com.kynetio.hollowtanvil.registry.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(HollowAnvilMod.MODID)
public class HollowAnvilMod {

    public static final String MODID = "hollowtanvil";

    public HollowAnvilMod(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        // ModBusEvents inner classes are auto-discovered via @EventBusSubscriber
    }
}
