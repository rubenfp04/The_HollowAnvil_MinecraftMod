package com.kynetio.hollowtanvil.events;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.client.ModKeyBindings;
import com.kynetio.hollowtanvil.client.PowerCooldownOverlay;
import com.kynetio.hollowtanvil.client.renderer.HollowAnvilRenderer;
import com.kynetio.hollowtanvil.client.screen.HollowAnvilScreen;
import com.kynetio.hollowtanvil.network.CPacketActivatePower;
import com.kynetio.hollowtanvil.network.CPacketChooseReward;
import com.kynetio.hollowtanvil.network.CPacketToggleAutoTrap;
import com.kynetio.hollowtanvil.network.SPacketCooldownSync;
import com.kynetio.hollowtanvil.registry.ModBlockEntities;
import com.kynetio.hollowtanvil.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModBusEvents {

    @EventBusSubscriber(modid = HollowAnvilMod.MODID)
    public static final class CommonModEvents {

        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar(HollowAnvilMod.MODID);
            registrar.playToServer(
                    CPacketToggleAutoTrap.TYPE,
                    CPacketToggleAutoTrap.STREAM_CODEC,
                    CPacketToggleAutoTrap::handle
            );
            registrar.playToServer(
                    CPacketChooseReward.TYPE,
                    CPacketChooseReward.STREAM_CODEC,
                    CPacketChooseReward::handle
            );
            registrar.playToServer(
                    CPacketActivatePower.TYPE,
                    CPacketActivatePower.STREAM_CODEC,
                    CPacketActivatePower::handle
            );
            registrar.playToClient(
                    SPacketCooldownSync.TYPE,
                    SPacketCooldownSync.STREAM_CODEC,
                    SPacketCooldownSync::handle
            );
        }
    }

    @EventBusSubscriber(modid = HollowAnvilMod.MODID, value = Dist.CLIENT)
    public static final class ClientModEvents {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.HOLLOW_ANVIL_MENU.get(), HollowAnvilScreen::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.HOLLOW_ANVIL_BE.get(),
                    HollowAnvilRenderer::new);
        }

        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            event.register(ModKeyBindings.ACTIVATE_POWER);
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, PowerCooldownOverlay.ID, new PowerCooldownOverlay());
        }
    }

    private ModBusEvents() {}
}
