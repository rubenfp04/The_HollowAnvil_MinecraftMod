package com.kynetio.hollowtanvil.registry;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.effect.BloodFrenzyEffect;
import com.kynetio.hollowtanvil.effect.SoulArmorEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, HollowAnvilMod.MODID);

    public static final DeferredHolder<MobEffect, BloodFrenzyEffect> BLOOD_FRENZY =
            MOB_EFFECTS.register("blood_frenzy", BloodFrenzyEffect::new);

    public static final DeferredHolder<MobEffect, SoulArmorEffect> SOUL_ARMOR =
            MOB_EFFECTS.register("soul_armor", SoulArmorEffect::new);

    private ModEffects() {}
}
