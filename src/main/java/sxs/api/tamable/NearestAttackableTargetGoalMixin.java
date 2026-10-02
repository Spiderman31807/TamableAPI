package sxs.api.tamable.mixin;

import sxs.api.tamable.Tamable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.LivingEntity;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {
	@Shadow
	protected LivingEntity target;
	@Shadow
	protected TargetingConditions targetConditions;

	private NearestAttackableTargetGoalMixin() {
		super(null, false, false);
	}

	@Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
	private void canUse(CallbackInfoReturnable<Boolean> callback) {
		if (this.mob instanceof Tamable tamable && tamable.isTame())
			callback.setReturnValue(false);
	}

	@Override
	public boolean canContinueToUse() {
		return this.targetConditions != null ? this.targetConditions.test(getServerLevel(this.mob), this.mob, this.target) : super.canContinueToUse();
	}
}