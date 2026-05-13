package com.kynetio.hollowtanvil.network;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CPacketChooseReward(BlockPos pos, int choiceIndex) implements CustomPacketPayload {

    public static final Type<CPacketChooseReward> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "choose_reward"));

    public static final StreamCodec<ByteBuf, CPacketChooseReward> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CPacketChooseReward::pos,
                    ByteBufCodecs.VAR_INT,  CPacketChooseReward::choiceIndex,
                    CPacketChooseReward::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CPacketChooseReward packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                BlockEntity be = sp.level().getBlockEntity(packet.pos());
                if (be instanceof HollowAnvilBlockEntity altar
                        && altar.getBlockPos().distToCenterSqr(sp.position()) < 64.0) {
                    altar.chooseReward(packet.choiceIndex(), sp);
                }
            }
        });
    }
}
