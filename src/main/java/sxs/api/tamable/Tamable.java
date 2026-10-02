package sxs.api.tamable;

import sxs.api.attachments.Attachments;

import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.DamageTypeTags;

public interface Tamable extends OwnableEntity {
	Level level();

	RandomSource getRandom();

	boolean isPassenger();

	double distanceToSqr(Entity entity);

	float getHealth();

	float getMaxHealth();

	void heal(float amount);

	void setJumping(boolean jumping);

	void gameEvent(Holder<GameEvent> event);

	PathNavigation getNavigation();

	LivingEntity getTarget();

	void setTarget(LivingEntity target);

	double getRandomX(double multiplier);

	double getRandomY();

	double getRandomZ(double multiplier);

	float getYRot();

	float getXRot();

	void snapTo(double x, double y, double z, float yRot, float xRot);

	default double tamedPanicSpeed() {
		return 1.5f;
	}

	default TagKey<DamageType> tamedPanicTriggers() {
		return DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES;
	}

	default double followOwnerSpeed() {
		return 1f;
	}
	
	default float followOwnerStart() {
		return 10f;
	}
	
	default float followOwnerStop() {
		return 2f;
	}

	default boolean isTamableFood(ItemStack stack) {
		return this instanceof Animal animal ? animal.isFood(stack) : stack.is(ItemTags.MEAT);
	}

	default boolean isTameItem(ItemStack stack) {
		return this.isTamableFood(stack);
	}

	default boolean canTame() {
		return this.getTarget() == null;
	}

	default InteractionResult tamableInteract(Player player, InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		Item item = itemStack.getItem();
		if (this.isTame()) {
			if (this.isTamableFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
				FoodProperties food = itemStack.get(DataComponents.FOOD);
				float f = food != null ? food.nutrition() : 1;
				this.heal(2.0F * f);
				Events.usePlayerItem(player, hand, itemStack);
				this.gameEvent(GameEvent.EAT);
				return InteractionResult.SUCCESS;
			}
			if (this.isOwnedBy(player)) {
				if (this instanceof Collarable collarable && item instanceof DyeItem dye)
					return collarable.collarInteract(player, itemStack, dye);
				this.setSitting(!this.isSitting());
				this.setJumping(false);
				this.getNavigation().stop();
				this.setTarget(null);
				return InteractionResult.SUCCESS.withoutItem();
			}
		} else if (!this.level().isClientSide() && this.isTameItem(itemStack) && this.canTame()) {
			itemStack.consume(1, player);
			this.tryToTame(player);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}

	default void tryToTame(Player player) {
		if (this.getRandom().nextInt(3) == 0) {
			this.tame(player);
			this.getNavigation().stop();
			this.setTarget(null);
			this.setSitting(true);
			this.level().broadcastEntityEvent((Entity) (Object) this, (byte) 7);
		} else {
			this.level().broadcastEntityEvent((Entity) (Object) this, (byte) 6);
		}
	}

	default void spawnTamingParticles(boolean success) {
		ParticleOptions particle = success ? ParticleTypes.HEART : ParticleTypes.SMOKE;
		RandomSource random = this.getRandom();
		for (int i = 0; i < 7; i++) {
			double deltaX = random.nextGaussian() * 0.02;
			double deltaY = random.nextGaussian() * 0.02;
			double deltaZ = random.nextGaussian() * 0.02;
			this.level().addParticle(particle, this.getRandomX(1), this.getRandomY() + 0.5, this.getRandomZ(1), deltaX, deltaY, deltaZ);
		}
	}

	default boolean isTame() {
		return Attachments.orElse(this, Events.Tamed, false);
	}

	default void setTame(boolean tamed, boolean applyEffects) {
		Attachments.set(this, Events.Tamed, tamed);
		if (applyEffects)
			this.applyTamingSideEffects();
	}

	default void applyTamingSideEffects() {
	}

	default boolean isInSittingPose() {
		return Attachments.orElse(this, Events.Sitting, false);
	}

	default void setInSittingPose(boolean sitting) {
		Attachments.set(this, Events.Sitting, sitting);
	}

	@Override
	default EntityReference<LivingEntity> getOwnerReference() {
		return Attachments.orElse(this, Events.Owner, null);
	}

	default void setOwner(LivingEntity entity) {
		this.setOwnerReference(entity == null ? null : new EntityReference(entity));
	}

	default void setOwnerReference(EntityReference<LivingEntity> entity) {
		if (entity == null)
			Attachments.reset(this, Events.Owner);
		else
			Attachments.set(this, Events.Owner, entity);
	}

	default void tame(Player player) {
		this.setTame(true, true);
		this.setOwner(player);
	}

	default boolean isOwnedBy(LivingEntity entity) {
		return entity.equals(this.getOwner());
	}

	default boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
		if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand)
			return false;
		if (target instanceof Wolf wolf)
			return !wolf.isTame() || wolf.getOwner() != owner;
		if (target instanceof Player player && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(player))
			return false;
		if (target instanceof AbstractHorse abstracthorse && abstracthorse.isTamed())
			return false;
		if (target instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame())
			return false;
		if (target instanceof Tamable tamable && tamable.isTame())
			return false;
		return true;
	}

	boolean isSitting();

	void setSitting(boolean sitting);

	default void tryToTeleportToOwner() {
		LivingEntity owner = this.getOwner();
		if (owner != null)
			this.teleportToAroundBlockPos(owner.blockPosition());
	}

	default boolean shouldTryTeleportToOwner() {
		LivingEntity owner = this.getOwner();
		return owner != null && this.distanceToSqr(this.getOwner()) >= 144;
	}

	default void teleportToAroundBlockPos(BlockPos pos) {
		RandomSource random = this.getRandom();
		for (int i = 0; i < 10; i++) {
			int xOffset = random.nextIntBetweenInclusive(-3, 3);
			int zOffset = random.nextIntBetweenInclusive(-3, 3);
			if (Math.abs(xOffset) >= 2 || Math.abs(zOffset) >= 2) {
				int yOffset = random.nextIntBetweenInclusive(-1, 1);
				if (this.maybeTeleportTo(pos.getX() + xOffset, pos.getY() + yOffset, pos.getZ() + zOffset))
					return;
			}
		}
	}

	default boolean maybeTeleportTo(int x, int y, int z) {
		if (!this.canTeleportTo(new BlockPos(x, y, z)))
			return false;
		this.snapTo(x + 0.5, y, z + 0.5, this.getYRot(), this.getXRot());
		this.getNavigation().stop();
		return true;
	}

	default boolean canTeleportTo(BlockPos pos) {
		Mob mob = (Mob) (Object) this;
		PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(mob, pos);
		if (pathtype != PathType.WALKABLE)
			return false;
		BlockState blockstate = this.level().getBlockState(pos.below());
		if (!this.canFlyToOwner() && blockstate.getBlock() instanceof LeavesBlock)
			return false;
		BlockPos blockpos = pos.subtract(mob.blockPosition());
		return this.level().noCollision(mob, mob.getBoundingBox().move(blockpos));
	}

	default boolean unableToMoveToOwner() {
		return this.isSitting() || this.isPassenger() || (this instanceof Leashable leashable && leashable.mayBeLeashed()) || this.getOwner() != null && this.getOwner().isSpectator();
	}

	default boolean canFlyToOwner() {
		return false;
	}
}