package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, HollowAnvilMod.MODID);

    /** Played when the altar absorbs soul essence from a dying mob. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_ABSORB =
            SOUND_EVENTS.register("soul_absorb", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "soul_absorb")));

    /** Played when the player activates a buff (legacy, kept for compatibility). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUFF_GRANTED =
            SOUND_EVENTS.register("buff_granted", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "buff_granted")));

    /** Played when the Hollow Anvil is placed for the first time — deep, ancient rumble. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_AWAKEN =
            SOUND_EVENTS.register("altar_awaken", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "altar_awaken")));

    /** Played when the altar grants a blood reward — triumphant and dark. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOD_REWARD =
            SOUND_EVENTS.register("blood_reward", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "blood_reward")));

    /** Played when the altar summons the cage around a prey. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CAGE_LOCK =
            SOUND_EVENTS.register("cage_lock", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, "cage_lock")));

    private ModSounds() {}
}
