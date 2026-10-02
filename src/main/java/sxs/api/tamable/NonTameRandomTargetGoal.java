package sxs.api.tamable;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

public class NonTameRandomTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
	private final Tamable tamable;

	public <M extends Mob & Tamable> NonTameRandomTargetGoal(M mob, Class<T> targetClass, boolean mustSee, TargetingConditions.Selector selector) {
		super(mob, targetClass, 10, mustSee, false, selector);
		this.tamable = mob;
	}

	@Override
	public boolean canUse() {
		return !this.tamable.isTame() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return this.targetConditions != null ? this.targetConditions.test(getServerLevel(this.mob), this.mob, this.target) : super.canContinueToUse();
	}
}