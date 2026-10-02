package sxs.api.tamable;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;

public class OwnerHurtTargetGoal extends TargetGoal {
	private final Tamable tamable;
	private LivingEntity ownerLastHurt;
	private int timestamp;

	public <M extends Mob & Tamable> OwnerHurtTargetGoal(M mob) {
		super(mob, false);
		this.tamable = mob;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (this.tamable.isTame() && !this.tamable.isSitting()) {
			LivingEntity owner = this.tamable.getOwner();
			if (owner == null)
				return false;

			this.ownerLastHurt = owner.getLastHurtMob();
			int timestamp = owner.getLastHurtMobTimestamp();
			return timestamp != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT) && this.tamable.wantsToAttack(this.ownerLastHurt, owner);
		}
		
		return false;
	}

	@Override
	public void start() {
		this.mob.setTarget(this.ownerLastHurt);
		LivingEntity owner = this.tamable.getOwner();
		if (owner != null)
			this.timestamp = owner.getLastHurtMobTimestamp();
		super.start();
	}
}