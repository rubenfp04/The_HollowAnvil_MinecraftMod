package com.kynetio.hollowtanvil.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SoulArmorEffect extends MobEffect {

    public SoulArmorEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2E0033);
        addAttributeModifier(
                Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath("hollowtanvil", "soul_armor_armor"),
                5.0, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(
                Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath("hollowtanvil", "soul_armor_health"),
                4.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        double x = entity.getX(), y = entity.getY() + 0.2, z = entity.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 2 + amplifier, 0.3, 0.1, 0.3, 0.01);
        level.sendParticles(ParticleTypes.ASH, x, y + entity.getBbHeight(), z, 3, 0.2, 0.1, 0.2, 0.02);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }
}
