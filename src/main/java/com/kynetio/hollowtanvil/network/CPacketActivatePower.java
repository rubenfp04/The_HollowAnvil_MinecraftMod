package com.kynetio.hollowtanvil.network;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.block.HollowAnvilBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CPacketActivatePower() implements CustomPacketPayload {

    public static final Type<CPacketActivatePower> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "activate_power"));

    public static final StreamCodec<ByteBuf, CPacketActivatePower> STREAM_CODEC =
            StreamCodec.unit(new CPacketActivatePower());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CPacketActivatePower packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                HollowAnvilBlockEntity.activatePower(sp);
            }
        });
    }
}
