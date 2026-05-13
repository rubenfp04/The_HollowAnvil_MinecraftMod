package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HollowAnvilMod.MODID);

    public static final Supplier<CreativeModeTab> HOLLOW_ANVIL_TAB =
            CREATIVE_MODE_TABS.register("main", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.hollowtanvil.main"))
                            .icon(() -> ModItems.HOLLOW_ANVIL.get().getDefaultInstance())
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.HOLLOW_ANVIL.get());
                            })
                            .build());

    private ModCreativeTabs() {}
}
