package sxs.api.tamable;

import net.neoforged.neoforge.common.NeoForge;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.DyeColor;

public interface Breedable extends Ageable {
	default double breedSpeed() {
		return 0.6f;
	}
	
	default Class<? extends Mob> getBreedingClass() {
		return (Class<? extends Mob>) this.getClass();
	}
	
	default boolean isBreedingFood(ItemStack itemStack) {
		return this instanceof Animal animal ? false : itemStack.is(ItemTags.MEAT);
	}

	default InteractionResult breedInteract(Player player, InteractionHand hand) {
		if(this instanceof Tamable tamable && !tamable.isTame())
			return InteractionResult.PASS;
		
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.isBreedingFood(itemStack)) {
            int age = this.getAge();
            if (player instanceof ServerPlayer serverPlayer && age == 0 && this.canFallInLove()) {
				Events.usePlayerItem(player, hand, itemStack);
                this.setInLove(serverPlayer);
                this.playEatingSound();
                return InteractionResult.SUCCESS_SERVER;
            }
            
            if (this.isBaby()) {
				Events.usePlayerItem(player, hand, itemStack);
                this.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-age), true);
                this.playEatingSound();
                return InteractionResult.SUCCESS;
            }
            
            if (this.level().isClientSide)
                return InteractionResult.CONSUME;
        }
        
        return InteractionResult.PASS;
    }

	default void playEatingSound() {
	}

	default boolean canFallInLove() {
		return !this.isInLove();
	}

	default void setInLove(Player player) {
		this.setInLoveTime(600);
		if (player instanceof ServerPlayer serverPlayer)
			this.setLoveCause(new EntityReference<>(serverPlayer));
		this.level().broadcastEntityEvent((Entity) this, (byte) 18);
	}

	abstract void setInLoveTime(int time);

	abstract int getInLoveTime();

	abstract void setLoveCause(EntityReference<ServerPlayer> cause);

	abstract ServerPlayer getLoveCause();

	default boolean isInLove() {
		return this.getInLoveTime() > 0;
	}

	default void resetLove() {
		this.setInLoveTime(0);
	}

	default boolean canMate(Breedable mate) {
		return mate == this || mate.getClass() != this.getClass() ? false : this.isInLove() && mate.isInLove();
	}

	default void spawnChildFromBreeding(ServerLevel serverLevel, Breedable mate) {
		Ageable child = this.getBreedOffspring(serverLevel, mate);
		if(child instanceof Tamable tamable && this instanceof Tamable tamableSource && tamableSource.isTame()) {
            tamable.setOwnerReference(tamableSource.getOwnerReference());
            tamable.setTame(true, true);
		}

		if(child instanceof Collarable collarable && this instanceof Collarable collarSource) {
			DyeColor color = collarSource.getCollarColor();
			if(mate instanceof Collarable collarMate)
				collarable.setCollarColor(DyeColor.getMixedColor(serverLevel, color, collarMate.getCollarColor()));
			else
				collarable.setCollarColor(color);
		}
		
		final BreedingSpawnEvent event = new BreedingSpawnEvent(this, mate, child);
		final boolean cancelled = NeoForge.EVENT_BUS.post(event).isCanceled();
		child = event.getChild();

		if (cancelled) {
			this.setAge(6000);
			mate.setAge(6000);
			this.resetLove();
			mate.resetLove();
			return;
		}

		if (child != null) {
			child.setBaby(true);
			Entity entity = (Entity) this;
			((Entity) child).snapTo(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
			this.finalizeSpawnChildFromBreeding(serverLevel, mate, child);
			serverLevel.addFreshEntityWithPassengers(((Entity) child));
		}
	}

	default void finalizeSpawnChildFromBreeding(ServerLevel serverLevel, Breedable mate, Ageable child) {
		this.setAge(6000);
		mate.setAge(6000);
		this.resetLove();
		mate.resetLove();
		
		serverLevel.broadcastEntityEvent((Entity) this, (byte) 18);
		if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
			Entity entity = (Entity) this;
			serverLevel.addFreshEntity(new ExperienceOrb(serverLevel, entity.getX(), entity.getY(), entity.getZ(), entity.getRandom().nextInt(7) + 1));
		}
	}

	Level level();
}