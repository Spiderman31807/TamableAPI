package sxs.api.tamable;

import sxs.api.attachments.AttachmentHolder;

import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

public interface Ageable {
	default Ageable getBreedOffspring(ServerLevel serverLevel, Ageable mate) {
		return ((Entity) this).getType().create(serverLevel, EntitySpawnReason.BREEDING) instanceof Ageable ageable ? ageable : null;
	}

	default boolean canBreed() {
		return false;
	}

	abstract void addForcedAge(int amount);

	abstract void setAgeD(int age);

	abstract int getAge();

	default void ageUp(int seconds, boolean forced) {
		int age = this.getAge();
		int original = age;
		age += seconds * 20;
		if (age > 0)
			age = 0;

		this.setAge(age);
		if (forced)
			this.addForcedAge(age - original);
		if (this.getAge() == 0)
			this.setAge(this.getForcedAge());
	}

	default void ageUp(int seconds) {
		this.ageUp(seconds, false);
	}

	default void setAge(int newAge) {
		int currentAge = this.getAge();
		this.setAgeD(newAge);
		if (currentAge < 0 && newAge >= 0 || currentAge >= 0 && newAge < 0) {
			((AttachmentHolder) this).setAttachmentValue(Events.IsBaby, newAge < 0);
			this.ageBoundaryReached();
		}
	}

	default void ageBoundaryReached() {
		Entity entity = (Entity) this;
		if (!this.isBaby() && entity.isPassenger() && entity.getVehicle() instanceof AbstractBoat boat && !boat.hasEnoughSpaceFor(entity))
			entity.stopRiding();
	}

	abstract int getForcedAge();

	abstract int getForcedAgeTimer();

	void setBaby(boolean isBaby);

	boolean isBaby();
}