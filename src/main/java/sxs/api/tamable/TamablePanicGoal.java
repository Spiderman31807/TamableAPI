package sxs.api.tamable;

import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.tags.TagKey;

public class TamablePanicGoal extends PanicGoal {
	private final Tamable tamable;

	public <M extends PathfinderMob & Tamable> TamablePanicGoal(M mob) {
     	super(mob, mob.tamedPanicSpeed(), mob.tamedPanicTriggers());
     	this.tamable = mob;
    }

	public <M extends PathfinderMob & Tamable> TamablePanicGoal(M mob, double speedModifier) {
    	super(mob, speedModifier);
     	this.tamable = mob;
    }

	@Override
	public void tick() {
		if (!this.tamable.unableToMoveToOwner() && this.tamable.shouldTryTeleportToOwner())
			this.tamable.tryToTeleportToOwner();
		super.tick();
	}
}