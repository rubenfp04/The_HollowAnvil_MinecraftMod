package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.block.HollowAnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(HollowAnvilMod.MODID);

    public static final DeferredBlock<HollowAnvilBlock> HOLLOW_ANVIL =
            BLOCKS.registerBlock("hollow_anvil", HollowAnvilBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .requiresCorrectToolForDrops()
                            .strength(5.0f, 1200.0f)
                            .sound(SoundType.ANVIL)
                            .noOcclusion()
            );

    private ModBlocks() {}
}
