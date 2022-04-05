package coda.whooosh.common;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class HotAirBalloonEntity extends Animal implements IAnimatable, IAnimationTickable {
    private static final EntityDataAccessor<Integer> LITNESS = SynchedEntityData.defineId(HotAirBalloonEntity.class, EntityDataSerializers.INT);
    private final AnimationFactory factory = new AnimationFactory(this);

    public HotAirBalloonEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5F).add(Attributes.ATTACK_DAMAGE, 0.0F);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LITNESS, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (getLitness() > 1 && isVehicle() && tickCount % 60 == 0 && random.nextBoolean()) {
            setLitness(getLitness() - 1);
        }

        if (!isVehicle() && getLitness() > 0 && random.nextBoolean()) {
            setLitness(getLitness() - 1);
        }

        if (isInWaterOrRain()) {
            setLitness(0);
        }

        if (tickCount % 10 == 0 && getLitness() > 0) {
            for (int i = 0; i < getLitness(); i++) {
                level.addParticle(ParticleTypes.FLAME, getX(), getY() + 2.35D,  getZ(), getDeltaMovement().x, 0.1D, getDeltaMovement().z);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {

        if (player.getItemInHand(hand).isEmpty()) {
            player.startRiding(this);
        }

        if (isVehicle() && getControllingPassenger().is(player)) {
            if (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL)) {
                setLitness(getLitness() + 1);
                swing(hand);
                player.getItemInHand(hand).hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
        }

        if (player.isShiftKeyDown()) {
            discard();
            //spawnAtLocation(new ItemStack(Items.APPLE));
            return InteractionResult.PASS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public Entity getControllingPassenger() {
        return this.getPassengers().isEmpty() ? null : this.getPassengers().get(0);
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public void travel(Vec3 pos) {
        if (isAlive()) {
            if (this.isVehicle()) {
                super.travel(move(pos));
            }
            else {
                super.travel(pos);
            }
        }
        super.travel(pos);
    }

    public void setLitness(int litness) {
        this.entityData.set(LITNESS, litness);
    }

    public int getLitness() {
        return Math.min(this.entityData.get(LITNESS), 5);
    }

    private Vec3 move(Vec3 pos) {
        if (!(getControllingPassenger() instanceof LivingEntity)) return Vec3.ZERO;

        LivingEntity passenger = (LivingEntity) this.getControllingPassenger();

        Vec3 view = passenger.getLookAngle().scale(0.125D);

        if (getLitness() > 1) {
            setDeltaMovement(pos.x + view.x, getLitness() * 0.02D, pos.z + view.z);
            if (isOnGround()) {
                setDeltaMovement(getDeltaMovement().add(0D, 1.0D, 0D));
            }
        }

        if (getLitness() == 1) {
            setDeltaMovement(pos.x + view.x, -0.025D, pos.z + view.z);
        }

        return pos;
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.5D;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p_20123_) {
        return new Vec3(this.getX(), this.getY(), this.getZ());
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source == DamageSource.OUT_OF_WORLD;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    @Override
    public void registerControllers(AnimationData data) {}

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    @Override
    public int tickTimer() {
        return tickCount;
    }
}
