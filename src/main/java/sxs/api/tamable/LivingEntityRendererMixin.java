package sxs.api.tamable.mixin;

import sxs.api.tamable.Ageable;
import sxs.api.tamable.HasBabyModel;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void extractRenderState(T entity, S state, float partialTick, CallbackInfo callback) {
		if (entity instanceof Ageable && !(entity instanceof HasBabyModel baby && baby.hasBabyModel()))
			state.scale *= state.ageScale;
	}
}