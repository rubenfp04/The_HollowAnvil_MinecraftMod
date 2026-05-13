package com.kynetio.hollowtanvil.network;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.client.PowerCooldownOverlay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SPacketCooldownSync(int remainingTicks, int totalTicks) implements CustomPacketPayload {

    public static final Type<SPacketCooldownSync> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "cooldown_sync"));

    public static final StreamCodec<ByteBuf, SPacketCooldownSync> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SPacketCooldownSync::remainingTicks,
                    ByteBufCodecs.VAR_INT, SPacketCooldownSync::totalTicks,
                    SPacketCooldownSync::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SPacketCooldownSync packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> PowerCooldownOverlay.setCooldown(packet.remainingTicks, packet.totalTicks));
    }
}
