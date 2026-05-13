package com.kynetio.hollowtanvil.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {

    public static final KeyMapping ACTIVATE_POWER = new KeyMapping(
            "key.hollowtanvil.activate_power",
            GLFW.GLFW_KEY_LEFT_ALT,
            new KeyMapping.Category(ResourceLocation.fromNamespaceAndPath("hollowtanvil", "hollowtanvil"))
    );

    private ModKeyBindings() {}
}
