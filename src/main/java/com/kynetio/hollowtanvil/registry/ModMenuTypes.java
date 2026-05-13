package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.menu.HollowAnvilMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, HollowAnvilMod.MODID);

    /**
     * IMenuTypeExtension.create() accepts a FriendlyByteBuf factory constructor,
     * which lets us send the BlockPos from server→client so the client-side
     * constructor can resolve the BlockEntity.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HollowAnvilMenu>>
            HOLLOW_ANVIL_MENU = MENU_TYPES.register("hollow_anvil_menu",
                    () -> IMenuTypeExtension.create(HollowAnvilMenu::new));

    private ModMenuTypes() {}
}
