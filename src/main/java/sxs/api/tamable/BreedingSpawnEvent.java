package sxs.api.tamable;

import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.player.Player;

public class BreedingSpawnEvent extends Event implements ICancellableEvent {
	private final Breedable parentA;
	private final Breedable parentB;
	private final Player causedByPlayer;
	private Ageable child;

	public BreedingSpawnEvent(Breedable parentA, Breedable parentB, Ageable proposedChild) {
		Player causedByPlayer = parentA.getLoveCause();
		if (causedByPlayer == null)
			causedByPlayer = parentB.getLoveCause();
		this.parentA = parentA;
		this.parentB = parentB;
		this.causedByPlayer = causedByPlayer;
		this.child = proposedChild;
	}

	public Breedable getParentA() {
		return parentA;
	}

	public Breedable getParentB() {
		return parentB;
	}

	public Player getCausedByPlayer() {
		return causedByPlayer;
	}

	public Ageable getChild() {
		return child;
	}

	public void setChild(Ageable proposedChild) {
		child = proposedChild;
	}
}