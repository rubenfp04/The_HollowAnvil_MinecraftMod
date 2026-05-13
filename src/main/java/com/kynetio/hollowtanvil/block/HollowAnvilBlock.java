package com.kynetio.hollowtanvil.block;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.registry.ModBlockEntities;
import com.kynetio.hollowtanvil.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HollowAnvilBlock extends BaseEntityBlock {

    public static final MapCodec<HollowAnvilBlock> CODEC = simpleCodec(HollowAnvilBlock::new);
    private static final int MAX_ALTARS_PER_PLAYER = 2;
    private static final double MIN_DISTANCE = 100.0;

    public HollowAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HollowAnvilBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.HOLLOW_ANVIL_BE.get(),
                    HollowAnvilBlockEntity::clientTick);
        }
        return createTickerHelper(type, ModBlockEntities.HOLLOW_ANVIL_BE.get(),
                HollowAnvilBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                             @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            ServerLevel srv = (ServerLevel) level;

            // Check existing altars owned by this player
            List<BlockPos> ownedAltars = findOwnedAltars(srv, player, pos);

            // Check distance to nearest altar
            for (BlockPos other : ownedAltars) {
                double dist = Math.sqrt(pos.distSqr(other));
                if (dist < MIN_DISTANCE) {
                    player.sendSystemMessage(
                            Component.translatable("message.hollowtanvil.altar_too_close", (int) dist, (int) MIN_DISTANCE)
                                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    level.destroyBlock(pos, false);
                    player.getInventory().placeItemBackInInventory(
                            new ItemStack(this.asItem()));
                    return;
                }
            }

            // Check max count
            if (ownedAltars.size() >= MAX_ALTARS_PER_PLAYER) {
                player.sendSystemMessage(
                        Component.translatable("message.hollowtanvil.altar_limit", MAX_ALTARS_PER_PLAYER)
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                level.destroyBlock(pos, false);
                player.getInventory().placeItemBackInInventory(
                        new ItemStack(this.asItem()));
                return;
            }

            level.playSound(null, pos, ModSounds.ALTAR_AWAKEN.get(),
                    SoundSource.BLOCKS, 3.0f, 0.6f);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HollowAnvilBlockEntity anvil) {
                anvil.setOwner(player);
            }

            net.minecraft.advancements.AdvancementHolder holder = srv.getServer().getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "first_altar"));
            if (holder != null) {
                player.getAdvancements().award(holder, "placed");
            }
        }
    }

    private List<BlockPos> findOwnedAltars(ServerLevel level, Player player, BlockPos exclude) {
        java.util.ArrayList<BlockPos> found = new java.util.ArrayList<>();
        // Scan loaded chunks in a radius around the player
        int chunkRadius = (int)(MIN_DISTANCE / 16) + 2;
        int cx = player.blockPosition().getX() >> 4;
        int cz = player.blockPosition().getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof HollowAnvilBlockEntity altar
                            && !be.getBlockPos().equals(exclude)
                            && player.getUUID().equals(altar.getOwnerUuid())) {
                        found.add(be.getBlockPos());
                    }
                }
            }
        }
        return found;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
