package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HollowAnvilMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HollowAnvilBlockEntity>>
            HOLLOW_ANVIL_BE = BLOCK_ENTITY_TYPES.register("hollow_anvil_be",
                    () -> new BlockEntityType<>(HollowAnvilBlockEntity::new, ModBlocks.HOLLOW_ANVIL.get()));

    private ModBlockEntities() {}
}
