package com.kynetio.hollowtanvil.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BloodFrenzyEffect extends MobEffect {

    public BloodFrenzyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xB50000);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath("hollowtanvil", "blood_frenzy_speed"),
                0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath("hollowtanvil", "blood_frenzy_damage"),
                2.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        double x = entity.getX(), y = entity.getY() + entity.getBbHeight() * 0.5, z = entity.getZ();
        level.sendParticles(ParticleTypes.FALLING_LAVA, x, y, z, 3 + amplifier, 0.3, 0.4, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + entity.getBbHeight(), z, 2, 0.15, 0.05, 0.15, 0.01);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 40 == 0;
    }
}
