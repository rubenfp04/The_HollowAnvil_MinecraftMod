package com.kynetio.hollowtanvil.block;

import com.kynetio.hollowtanvil.HollowAnvilMod;
import com.kynetio.hollowtanvil.registry.ModBlockEntities;
import com.kynetio.hollowtanvil.registry.ModEffects;
import com.kynetio.hollowtanvil.registry.ModSounds;
import com.kynetio.hollowtanvil.reward.RewardType;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HollowAnvilBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_ESSENCE    = 10_000;
    private static final int SCAN_INTERVAL      = 10;
    private static final int DEMAND_MIN         = 2_000;
    private static final int DEMAND_MAX         = 8_000;
    private static final int MAX_CAGED_MOBS     = 3;
    private static final int DRAG_DURATION      = 40;
    private static final double ABSORB_RANGE    = 6.0;

    private int storedEssence = 0;
    private int bloodDemand   = 0;
    private int tickCounter   = 0;
    private boolean autoTrap    = true;

    private boolean pendingReward = false;
    private final int[] pendingChoices = new int[]{ -1, -1, -1 };

    private @Nullable UUID ownerUuid = null;
    private String ownerName = "";
    private int demandsMet = 0;

    private static class CagedMob {
        UUID targetId;
        BlockPos center;
        int dragTimer;

        CagedMob(UUID id, BlockPos center) {
            this.targetId = id;
            this.center = center;
            this.dragTimer = DRAG_DURATION;
        }
    }

    private static class TravelingSoul {
        final Vec3 start;
        final Vec3 end;
        int age;
        final int maxAge;

        TravelingSoul(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
            this.age = 0;
            this.maxAge = 40;
        }
    }

    private final List<TravelingSoul> travelingSouls = new ArrayList<>();
    private final List<CagedMob> cagedMobs = new ArrayList<>();

    public HollowAnvilBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.HOLLOW_ANVIL_BE.get(), pos, blockState);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getGameProfile().name();
        setChanged();
    }

    public @Nullable UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public int getDemandsMet() { return demandsMet; }

    public int getProgressiveTier() {
        if (demandsMet >= 10) return 4;
        if (demandsMet >= 6) return 3;
        if (demandsMet >= 3) return 2;
        return 1;
    }

    private boolean isOwnerKill(LivingEntity dead) {
        if (ownerUuid == null) return true;
        LivingEntity attacker = dead.getLastHurtByMob();
        if (attacker instanceof Player p) {
            return p.getUUID().equals(ownerUuid);
        }
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   HollowAnvilBlockEntity be) {
        if (be.bloodDemand == 0) {
            be.bloodDemand = level.random.nextIntBetweenInclusive(DEMAND_MIN, DEMAND_MAX);
            be.setChanged();
        }

        if (!be.pendingReward && be.storedEssence >= be.bloodDemand) {
            be.rollPendingChoices(level.random);
            be.pendingReward = true;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);

            AABB notifyBox = new AABB(pos).inflate(12.0);
            for (ServerPlayer sp : ((ServerLevel) level).getEntitiesOfClass(ServerPlayer.class, notifyBox)) {
                sp.sendSystemMessage(
                        Component.translatable("message.hollowtanvil.choose_reward")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            level.playSound(null, pos, ModSounds.BLOOD_REWARD.get(),
                    SoundSource.BLOCKS, 2.0f, 1.0f);
        }

        Iterator<TravelingSoul> soulIt = be.travelingSouls.iterator();
        while (soulIt.hasNext()) {
            TravelingSoul soul = soulIt.next();
            soul.age++;
            double t = (double) soul.age / soul.maxAge;
            double px = Mth.lerp(t, soul.start.x, soul.end.x);
            double py = Mth.lerp(t, soul.start.y, soul.end.y) + Math.sin(t * Math.PI) * 0.8;
            double pz = Mth.lerp(t, soul.start.z, soul.end.z);

            double spiralAngle = t * Math.PI * 6.0;
            double sr = 0.4 * (1.0 - t);
            double sx = px + Math.cos(spiralAngle) * sr;
            double sz = pz + Math.sin(spiralAngle) * sr;

            ServerLevel srv2 = (ServerLevel) level;
            srv2.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, sx, py, sz, 3, 0.03, 0.03, 0.03, 0.002);
            srv2.sendParticles(ParticleTypes.SOUL, sx, py, sz, 1, 0.01, 0.01, 0.01, 0.001);

            if (soul.age >= soul.maxAge) {
                soulIt.remove();
                spawnAbsorptionBurst(srv2, pos);
            }
        }

        Iterator<CagedMob> it = be.cagedMobs.iterator();
        while (it.hasNext()) {
            CagedMob cage = it.next();
            Entity target = ((ServerLevel) level).getEntity(cage.targetId);

            if (cage.dragTimer > 0) {
                if (target instanceof LivingEntity living && living.isAlive()) {
                    Vec3 center = Vec3.atBottomCenterOf(cage.center);
                    Vec3 mobPos = living.position();
                    Vec3 pull = center.subtract(mobPos);
                    double dist = pull.horizontalDistance();

                    if (dist > 0.15) {
                        Vec3 drag = pull.normalize().scale(Math.min(dist, 0.25));
                        living.setDeltaMovement(drag.x, living.getDeltaMovement().y, drag.z);
                    } else {
                        living.teleportTo(center.x, living.getY(), center.z);
                        living.setDeltaMovement(Vec3.ZERO);
                    }

                    ServerLevel srv = (ServerLevel) level;
                    for (int i = 0; i < 3; i++) {
                        double tt = level.random.nextDouble();
                        double ppx = Mth.lerp(tt, mobPos.x, center.x) + level.random.nextGaussian() * 0.1;
                        double ppy = mobPos.y + living.getBbHeight() * 0.5 + level.random.nextGaussian() * 0.2;
                        double ppz = Mth.lerp(tt, mobPos.z, center.z) + level.random.nextGaussian() * 0.1;
                        srv.sendParticles(ParticleTypes.DUST_PLUME, ppx, ppy, ppz, 1, 0, 0, 0, 0);
                    }
                    double angle = (level.getGameTime() % 20) * (2.0 * Math.PI / 20.0);
                    srv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            mobPos.x + Math.cos(angle) * 0.6,
                            mobPos.y + 0.5,
                            mobPos.z + Math.sin(angle) * 0.6,
                            1, 0, 0.02, 0, 0);

                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 9, false, false));
                }
                cage.dragTimer--;
                continue;
            }

            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3 center = Vec3.atBottomCenterOf(cage.center);
                Vec3 diff = living.position().subtract(center);
                if (diff.x * diff.x + diff.z * diff.z > 0.25) {
                    living.teleportTo(center.x, living.getY(), center.z);
                }
                living.setDeltaMovement(Vec3.ZERO);
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 9, false, false));

                ServerLevel srv3 = (ServerLevel) level;
                double angle2 = (level.getGameTime() % 20) * (2.0 * Math.PI / 20.0);
                Vec3 mobPos2 = living.position();
                srv3.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        mobPos2.x + Math.cos(angle2) * 0.6,
                        mobPos2.y + 0.5,
                        mobPos2.z + Math.sin(angle2) * 0.6,
                        1, 0, 0.02, 0, 0);
                if (level.getGameTime() % 10 == 0) {
                    srv3.sendParticles(ParticleTypes.SOUL,
                            mobPos2.x, mobPos2.y + living.getBbHeight() * 0.5, mobPos2.z,
                            2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (target == null
                    || (target instanceof LivingEntity le && !le.isAlive())) {
                it.remove();
            }
        }

        be.tickCounter++;
        if (be.tickCounter < SCAN_INTERVAL) return;
        be.tickCounter = 0;

        ServerLevel srv = (ServerLevel) level;

        AABB absorbBox = new AABB(pos).inflate(ABSORB_RANGE);
        List<LivingEntity> dying = level.getEntitiesOfClass(LivingEntity.class, absorbBox,
                e -> !(e instanceof Player) && (!e.isAlive() || e.deathTime > 0) && be.isOwnerKill(e));

        if (!dying.isEmpty()) {
            int gained = 0;
            for (LivingEntity entity : dying) {
                gained += (int) (entity.getMaxHealth() * 10);
                Vec3 mobCenter = entity.position().add(0, entity.getBbHeight() / 2.0, 0);
                Vec3 altarCenter = Vec3.atCenterOf(pos).add(0, 0.8, 0);
                be.travelingSouls.add(new TravelingSoul(mobCenter, altarCenter));
                spawnSoulExplosion(srv, entity);
            }
            int before = be.storedEssence;
            be.storedEssence = Math.min(MAX_ESSENCE, be.storedEssence + gained);
            if (be.storedEssence != before) {
                float pitch = 0.65f + level.random.nextFloat() * 0.25f;
                level.playSound(null, pos, ModSounds.SOUL_ABSORB.get(),
                        SoundSource.BLOCKS, 1.8f, pitch);
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);

                AABB advBox = new AABB(pos).inflate(12.0);
                List<ServerPlayer> nearbyPlayers = srv.getEntitiesOfClass(ServerPlayer.class, advBox);
                for (ServerPlayer sp : nearbyPlayers) {
                    awardAdvancement(sp, "first_sacrifice", "absorbed");
                    sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 5, 0, false, true));
                }
                if (be.storedEssence >= MAX_ESSENCE) {
                    for (ServerPlayer sp : nearbyPlayers) {
                        awardAdvancement(sp, "blood_master", "maxed");
                    }
                }
            }
        }

        if (be.autoTrap && be.cagedMobs.size() < MAX_CAGED_MOBS) {
            AABB mobBox    = new AABB(pos).inflate(5.0);
            AABB playerBox = new AABB(pos).inflate(8.0);
            List<UUID> alreadyCaged = be.cagedMobs.stream().map(c -> c.targetId).toList();
            List<LivingEntity> nearbyMobs = level.getEntitiesOfClass(LivingEntity.class, mobBox,
                    e -> !(e instanceof Player) && e.isAlive() && !alreadyCaged.contains(e.getUUID()));
            List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, playerBox,
                    p -> p.isAlive() && !p.isSpectator());
            if (!nearbyPlayers.isEmpty()) {
                int slotsAvailable = MAX_CAGED_MOBS - be.cagedMobs.size();
                for (int i = 0; i < Math.min(slotsAvailable, nearbyMobs.size()); i++) {
                    be.startCage(level, pos, nearbyMobs.get(i));
                }
            }
        }
    }

    private void startCage(Level level, BlockPos altarPos, LivingEntity target) {
        CagedMob cage = new CagedMob(target.getUUID(), target.blockPosition());
        cagedMobs.add(cage);

        level.playSound(null, altarPos, ModSounds.CAGE_LOCK.get(),
                SoundSource.BLOCKS, 2.0f, 0.65f);
    }

    public void onRemoved(Level level) {
        cagedMobs.clear();
    }

    private void rollPendingChoices(RandomSource rng) {
        int count = RewardType.count();
        List<Integer> bag = new ArrayList<>();
        for (int i = 0; i < count; i++) bag.add(i);
        for (int i = 0; i < 3; i++) {
            int idx = rng.nextInt(bag.size());
            pendingChoices[i] = bag.remove(idx);
        }
    }

    public void chooseReward(int choiceIndex, ServerPlayer player) {
        if (!pendingReward) return;
        if (choiceIndex < 0 || choiceIndex >= pendingChoices.length) return;
        RewardType chosen = RewardType.byId(pendingChoices[choiceIndex]);
        if (chosen == null) return;
        if (!(level instanceof ServerLevel srv)) return;

        storedEssence -= bloodDemand;
        if (storedEssence < 0) storedEssence = 0;
        bloodDemand = srv.random.nextIntBetweenInclusive(DEMAND_MIN, DEMAND_MAX);

        pendingReward = false;
        pendingChoices[0] = pendingChoices[1] = pendingChoices[2] = -1;

        demandsMet++;
        int tier = getProgressiveTier();

        BlockPos pos = getBlockPos();
        spawnRewardFanfare(srv, pos);

        float durationMult = switch (tier) {
            case 2 -> 1.3f;
            case 3 -> 1.7f;
            case 4 -> 2.5f;
            default -> 1.0f;
        };
        int ampBoost = tier - 1;

        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH,  (int)(20 * 60 * 10 * durationMult), 2 + ampBoost));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,  (int)(20 * 60 *  5 * durationMult), 1 + ampBoost));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,    (int)(20 * 60 *  8 * durationMult), 3 + ampBoost));
        player.addEffect(new MobEffectInstance(ModEffects.BLOOD_FRENZY,  (int)(20 * 60 *  5 * durationMult), ampBoost));
        player.addEffect(new MobEffectInstance(ModEffects.SOUL_ARMOR,    (int)(20 * 60 *  3 * durationMult), ampBoost));
        player.sendSystemMessage(
                Component.translatable("message.hollowtanvil.favor_granted")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        awardAdvancement(player, "blood_pact", "granted");

        applyReward(chosen, srv, pos, player, tier);

        setChanged();
        srv.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    private static void spawnRewardFanfare(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 80, 0.5, 0.7, 0.5, 0.18);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,   cx, cy, cz, 60, 0.4, 0.6, 0.4, 0.22);
        level.sendParticles(ParticleTypes.SOUL,            cx, cy, cz, 40, 0.5, 0.6, 0.5, 0.08);
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),   cx, cy, cz, 30, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, pos, ModSounds.BLOOD_REWARD.get(), SoundSource.BLOCKS, 3.0f, 0.8f);
    }

    private static final String POWER_TAG = HollowAnvilMod.MODID + ":power";
    private static final String POWER_COOLDOWN_TAG = HollowAnvilMod.MODID + ":power_cd";

    private static int getCooldownTicks(RewardType type) {
        return switch (type) {
            case POWER_ENDERMAN -> 0;
            case POWER_GHAST    -> 20 * 4;
            case POWER_WITHER   -> 20 * 8;
            case POWER_CREEPER  -> 20 * 10;
            case POWER_BLAZE    -> 20 * 6;
            case POWER_SPIDER   -> 20 * 3;
            default -> 20 * 5;
        };
    }

    private static void applyReward(RewardType type, ServerLevel level, BlockPos pos, ServerPlayer player, int tier) {
        int itemMult = tier;
        switch (type) {
            case GOLDEN_APPLE  -> dropItem(level, pos, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, itemMult));
            case HOLLOW_SWORD  -> dropItem(level, pos, new ItemStack(Items.NETHERITE_SWORD));
            case HOLLOW_TOTEM  -> dropItem(level, pos, new ItemStack(Items.TOTEM_OF_UNDYING, itemMult));
            case BLOOD_INGOTS  -> dropItem(level, pos, new ItemStack(Items.NETHERITE_INGOT, 3 * itemMult));
            case POWER_ENDERMAN, POWER_GHAST, POWER_WITHER, POWER_CREEPER, POWER_BLAZE, POWER_SPIDER -> grantPower(player, type);
        }
    }

    private static void grantPower(ServerPlayer player, RewardType powerType) {
        CompoundTag data = player.getPersistentData();
        data.putInt(POWER_TAG, powerType.ordinal());
        data.putLong(POWER_COOLDOWN_TAG, 0);
        String msgKey = switch (powerType) {
            case POWER_ENDERMAN -> "message.hollowtanvil.power_enderman";
            case POWER_GHAST    -> "message.hollowtanvil.power_ghast";
            case POWER_WITHER   -> "message.hollowtanvil.power_wither";
            case POWER_CREEPER  -> "message.hollowtanvil.power_creeper";
            case POWER_BLAZE    -> "message.hollowtanvil.power_blaze";
            case POWER_SPIDER   -> "message.hollowtanvil.power_spider";
            default -> "";
        };
        player.sendSystemMessage(
                Component.translatable(msgKey)
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        player.sendSystemMessage(
                Component.translatable("message.hollowtanvil.power_hint")
                        .withStyle(ChatFormatting.GRAY), false);
    }

    public static void activatePower(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int powerId = data.getIntOr(POWER_TAG, -1);
        if (powerId < 0) return;
        RewardType type = RewardType.byId(powerId);
        if (type == null || !type.isPower()) return;

        int cooldown = getCooldownTicks(type);
        if (cooldown > 0) {
            long lastUse = data.getLongOr(POWER_COOLDOWN_TAG, 0L);
            long now = player.level().getGameTime();
            if (now - lastUse < cooldown) return;
            data.putLong(POWER_COOLDOWN_TAG, now);
            player.connection.send(new com.kynetio.hollowtanvil.network.SPacketCooldownSync(cooldown, cooldown));
        }
        ServerLevel srv = (ServerLevel) player.level();

        switch (type) {
            case POWER_ENDERMAN -> endermanTeleport(srv, player);
            case POWER_GHAST    -> ghastFireball(srv, player);
            case POWER_WITHER   -> witherVolley(srv, player);
            case POWER_CREEPER  -> creeperExplosion(srv, player);
            case POWER_BLAZE    -> blazeFireRain(srv, player);
            case POWER_SPIDER   -> spiderWeb(srv, player);
            default -> {}
        }
    }

    public static int getRemainingCooldownTicks(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int powerId = data.getIntOr(POWER_TAG, -1);
        if (powerId < 0) return 0;
        RewardType type = RewardType.byId(powerId);
        if (type == null || !type.isPower()) return 0;
        int cooldown = getCooldownTicks(type);
        if (cooldown <= 0) return 0;
        long lastUse = data.getLongOr(POWER_COOLDOWN_TAG, 0L);
        long now = player.level().getGameTime();
        long elapsed = now - lastUse;
        return elapsed >= cooldown ? 0 : (int)(cooldown - elapsed);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        boolean hadPower = data.getIntOr(POWER_TAG, -1) >= 0;
        data.remove(POWER_TAG);
        data.remove(POWER_COOLDOWN_TAG);

        ServerLevel srv = (ServerLevel) player.level();
        BlockPos.betweenClosedStream(
                player.blockPosition().offset(-64, -64, -64),
                player.blockPosition().offset(64, 64, 64)
        ).forEach(pos -> {
            if (srv.getBlockEntity(pos) instanceof HollowAnvilBlockEntity altar
                    && player.getUUID().equals(altar.ownerUuid)) {
                altar.demandsMet = 0;
                altar.setChanged();
                srv.sendBlockUpdated(pos, altar.getBlockState(), altar.getBlockState(), 3);
            }
        });

        if (hadPower) {
            String[] msgs = {
                "message.hollowtanvil.death_taunt_1",
                "message.hollowtanvil.death_taunt_2",
                "message.hollowtanvil.death_taunt_3",
                "message.hollowtanvil.death_taunt_4",
                "message.hollowtanvil.death_taunt_5"
            };
            String key = msgs[srv.random.nextInt(msgs.length)];
            player.sendSystemMessage(
                    Component.translatable(key)
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    private static void dropItem(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity ie = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, stack);
        ie.setPickUpDelay(20);
        ie.setDeltaMovement(0, 0.3, 0);
        level.addFreshEntity(ie);
    }

    private static void endermanTeleport(ServerLevel level, ServerPlayer player) {
        double range = 48.0;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        net.minecraft.world.phys.BlockHitResult hitResult = level.clip(
                new net.minecraft.world.level.ClipContext(
                        eyePos, endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                )
        );

        double ox = player.getX(), oy = player.getY(), oz = player.getZ();
        Vec3 targetPos;

        if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            BlockPos landingPos = hitResult.getBlockPos().relative(hitResult.getDirection());
            targetPos = Vec3.atBottomCenterOf(landingPos);
        } else {
            targetPos = endPos;
        }

        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        level.sendParticles(ParticleTypes.PORTAL, ox, oy + 1.0, oz, 60, 0.5, 1.0, 0.5, 0.5);
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.5, 1.0, 0.5, 0.5);
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void ghastFireball(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        net.minecraft.world.entity.projectile.LargeFireball fireball =
                new net.minecraft.world.entity.projectile.LargeFireball(level, player, look.scale(1.0), 1);
        fireball.setPos(
                player.getX() + look.x * 1.5,
                player.getEyeY() + look.y * 1.5,
                player.getZ() + look.z * 1.5);
        level.addFreshEntity(fireball);
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.GHAST_SHOOT,
                SoundSource.PLAYERS, 2.0f, 1.0f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX() + look.x, player.getEyeY() + look.y, player.getZ() + look.z,
                15, 0.3, 0.3, 0.3, 0.05);
    }

    private static void witherVolley(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        for (int i = -1; i <= 1; i++) {
            double spread = i * 0.15;
            Vec3 dir = new Vec3(look.x + spread, look.y, look.z - spread).normalize();
            net.minecraft.world.entity.projectile.WitherSkull skull =
                    new net.minecraft.world.entity.projectile.WitherSkull(level, player, dir);
            skull.setPos(
                    player.getX() + dir.x * 1.5,
                    player.getEyeY() + dir.y * 1.5,
                    player.getZ() + dir.z * 1.5);
            level.addFreshEntity(skull);
        }
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_SHOOT,
                SoundSource.PLAYERS, 2.0f, 1.0f);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getEyeY(), player.getZ(),
                25, 0.4, 0.4, 0.4, 0.05);
    }

    private static void creeperExplosion(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double tx = player.getX() + look.x * 5.0;
        double ty = player.getEyeY() + look.y * 5.0;
        double tz = player.getZ() + look.z * 5.0;
        level.explode(player, tx, ty, tz, 3.0f,
                Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tx, ty, tz, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 2.0f, 1.0f);
    }

    private static void blazeFireRain(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 8; i++) {
            double spread = (level.random.nextDouble() - 0.5) * 0.3;
            Vec3 dir = new Vec3(look.x + spread, look.y + 0.1, look.z + spread).normalize();
            net.minecraft.world.entity.projectile.SmallFireball fireball =
                    new net.minecraft.world.entity.projectile.SmallFireball(level, player, dir.scale(1.0));
            fireball.setPos(
                    player.getX() + dir.x * 1.5,
                    player.getEyeY() + dir.y * 1.5 + i * 0.15,
                    player.getZ() + dir.z * 1.5);
            level.addFreshEntity(fireball);
        }
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 2.0f, 1.0f);
        level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getEyeY(), player.getZ(),
                30, 0.5, 0.5, 0.5, 0.1);
    }

    private static void spiderWeb(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 10, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 10, 1, false, true));

        BlockPos target = player.blockPosition().offset(
                (int)(look.x * 6), (int)(look.y * 6), (int)(look.z * 6));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos webPos = target.offset(dx, 0, dz);
                if (level.getBlockState(webPos).isAir()) {
                    level.setBlock(webPos, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), 3);
                    level.scheduleTick(webPos, net.minecraft.world.level.block.Blocks.COBWEB, 20 * 5);
                }
            }
        }
        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.SPIDER_AMBIENT,
                SoundSource.PLAYERS, 2.0f, 0.8f);
        level.sendParticles(ParticleTypes.ITEM_COBWEB,
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                20, 1.0, 0.5, 1.0, 0.05);
    }

    private static void awardAdvancement(ServerPlayer player, String name, String criterion) {
        AdvancementHolder holder = ((ServerLevel) player.level()).getServer().getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(HollowAnvilMod.MODID, name));
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   HollowAnvilBlockEntity be) {
        if (be.storedEssence <= 0) return;

        long time = level.getGameTime();
        int tier = be.getProgressiveTier();
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
        RandomSource rand = level.random;

        if (time % 4 == 0) {
            double angle  = (time % 80) * (2.0 * Math.PI / 80.0);
            double radius = 0.35;
            double wx = cx + Math.cos(angle) * radius;
            double wy = pos.getY() + 1.15;
            double wz = cz + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.SOUL, wx, wy, wz, 0, 0.015, 0);
        }
        if (rand.nextInt(30) == 0) {
            double x = pos.getX() + 0.2 + rand.nextDouble() * 0.6;
            double z = pos.getZ() + 0.2 + rand.nextDouble() * 0.6;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, cy, z, 0, 0.01, 0);
        }

        if (tier >= 2) {
            if (time % 3 == 0) {
                double angle = (time % 40) * (2.0 * Math.PI / 40.0);
                double r = 0.5;
                level.addParticle(ParticleTypes.FLAME,
                        cx + Math.cos(angle) * r, cy + 0.3, cz + Math.sin(angle) * r,
                        0, 0.02, 0);
            }
            if (rand.nextInt(15) == 0) {
                level.addParticle(ParticleTypes.ENCHANTED_HIT,
                        cx + rand.nextGaussian() * 0.3,
                        cy + 0.5 + rand.nextDouble() * 0.5,
                        cz + rand.nextGaussian() * 0.3, 0, 0.04, 0);
            }
        }

        if (tier >= 3) {
            if (time % 2 == 0) {
                double angle = (time % 30) * (2.0 * Math.PI / 30.0);
                double r = 0.65;
                level.addParticle(ParticleTypes.FLAME,
                        cx + Math.cos(angle) * r, cy + 0.1, cz + Math.sin(angle) * r,
                        0, 0.01, 0);
                double angle2 = angle + Math.PI;
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        cx + Math.cos(angle2) * r, cy + 0.6, cz + Math.sin(angle2) * r,
                        0, 0.03, 0);
            }
            if (rand.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.LAVA,
                        cx + rand.nextGaussian() * 0.3,
                        cy + rand.nextDouble() * 0.4,
                        cz + rand.nextGaussian() * 0.3, 0, 0, 0);
            }
        }

        if (tier >= 4) {
            if (time % 2 == 0) {
                double angle = (time % 24) * (2.0 * Math.PI / 24.0);
                double r = 0.75;
                level.addParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                        cx + Math.cos(angle) * r, cy + 0.1, cz + Math.sin(angle) * r,
                        0, 0.015, 0);
            }
            if (rand.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SCULK_SOUL,
                        cx + rand.nextGaussian() * 0.2,
                        cy + rand.nextDouble() * 0.8,
                        cz + rand.nextGaussian() * 0.2, 0, 0.05, 0);
            }
            if (rand.nextInt(12) == 0) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL,
                        cx + rand.nextGaussian() * 0.4,
                        cy + rand.nextDouble() * 1.0,
                        cz + rand.nextGaussian() * 0.4, 0, 0.02, 0);
            }
            if (rand.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.PORTAL,
                        cx + rand.nextGaussian() * 0.5,
                        cy + rand.nextDouble() * 1.2,
                        cz + rand.nextGaussian() * 0.5,
                        rand.nextGaussian() * 0.02, 0.05, rand.nextGaussian() * 0.02);
            }
        }

        if (be.storedEssence > MAX_ESSENCE * 0.9 && rand.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.SOUL,
                    cx + rand.nextGaussian() * 0.4,
                    cy + rand.nextDouble() * 0.5,
                    cz + rand.nextGaussian() * 0.4, 0, 0.02, 0);
        }
    }

    private static void spawnSoulExplosion(ServerLevel level, LivingEntity entity) {
        Vec3 start = entity.position().add(0, entity.getBbHeight() / 2.0, 0);

        level.sendParticles(ParticleTypes.SOUL,            start.x, start.y, start.z, 25, 0.3, 0.4, 0.3, 0.12);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, start.x, start.y, start.z, 15, 0.2, 0.3, 0.2, 0.08);
        level.sendParticles(ParticleTypes.SCULK_SOUL,      start.x, start.y, start.z, 8,  0.1, 0.1, 0.1, 0.05);
        level.sendParticles(ParticleTypes.ASH,             start.x, start.y, start.z, 20, 0.4, 0.5, 0.4, 0.04);

        for (int r = 0; r < 8; r++) {
            double angle = r * (2.0 * Math.PI / 8.0);
            double rx = start.x + Math.cos(angle) * 0.5;
            double rz = start.z + Math.sin(angle) * 0.5;
            level.sendParticles(ParticleTypes.SOUL, rx, start.y, rz, 1, 0, 0.15, 0, 0.02);
        }
    }

    private static void spawnAbsorptionBurst(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy, cz, 40, 0.5, 0.6, 0.5, 0.12);
        level.sendParticles(ParticleTypes.SOUL,            cx, cy, cz, 25, 0.4, 0.5, 0.4, 0.06);
        level.sendParticles(ParticleTypes.SCULK_SOUL,      cx, cy + 0.3, cz, 6, 0.15, 0.2, 0.15, 0.03);

        for (int i = 0; i < 16; i++) {
            double angle = i * (2.0 * Math.PI / 16.0);
            double r = 0.7;
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    cx + Math.cos(angle) * r, cy + 0.1, cz + Math.sin(angle) * r,
                    1, 0, 0.08, 0, 0.04);
        }

        for (int y = 0; y < 5; y++) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    cx, cy + y * 0.3, cz, 3, 0.08, 0.05, 0.08, 0.01);
        }

        level.sendParticles(ParticleTypes.ASH,           cx, cy, cz, 20, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), cx, cy, cz, 10, 0.2, 0.3, 0.2, 0.02);
    }

    public int  getStoredEssence() { return storedEssence; }
    public int  getBloodDemand()   { return bloodDemand; }

    public void setStoredEssence(int value) {
        storedEssence = Math.max(0, Math.min(MAX_ESSENCE, value));
        setChanged();
    }

    public void setBloodDemand(int value) {
        bloodDemand = Math.max(0, value);
        setChanged();
    }

    public boolean isAutoTrap()          { return autoTrap; }
    public void setAutoTrap(boolean val) {
        autoTrap = val;
        if (!val) cagedMobs.clear();
        setChanged();
    }

    public boolean isPendingReward()     { return pendingReward; }
    public int     getPendingChoice(int i) {
        return (i >= 0 && i < pendingChoices.length) ? pendingChoices[i] : -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("storedEssence", storedEssence);
        output.putInt("bloodDemand",   bloodDemand);
        output.putBoolean("autoTrap", autoTrap);
        output.putBoolean("pendingReward", pendingReward);
        output.putIntArray("pendingChoices", new int[]{
                pendingChoices[0], pendingChoices[1], pendingChoices[2]
        });
        output.putInt("demandsMet", demandsMet);
        if (ownerUuid != null) {
            output.store("ownerUuid", UUIDUtil.CODEC, ownerUuid);
            output.putString("ownerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedEssence = input.getIntOr("storedEssence", 0);
        bloodDemand   = input.getIntOr("bloodDemand", 0);
        autoTrap      = input.getBooleanOr("autoTrap", true);
        pendingReward = input.getBooleanOr("pendingReward", false);
        input.getIntArray("pendingChoices").ifPresent(arr -> {
            for (int i = 0; i < pendingChoices.length; i++) {
                pendingChoices[i] = (i < arr.length) ? arr[i] : -1;
            }
        });
        demandsMet = input.getIntOr("demandsMet", 0);
        input.read("ownerUuid", UUIDUtil.CODEC).ifPresent(uuid -> {
            ownerUuid = uuid;
            ownerName = input.getStringOr("ownerName", "");
        });
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hollowtanvil.hollow_anvil");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.kynetio.hollowtanvil.menu.HollowAnvilMenu(containerId, playerInventory, this);
    }
}
