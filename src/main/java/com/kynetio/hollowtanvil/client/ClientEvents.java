package com.kynetio.hollowtanvil.client;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.network.CPacketActivatePower;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = HollowAnvilMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyBindings.ACTIVATE_POWER.consumeClick()) {
            ClientPacketDistributor.sendToServer(new CPacketActivatePower());
        }
        PowerCooldownOverlay.clientTick();
    }

    private ClientEvents() {}
}
