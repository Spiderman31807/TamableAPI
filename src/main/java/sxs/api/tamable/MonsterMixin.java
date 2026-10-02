package sxs.api.tamable.mixin;

import sxs.api.tamable.Tamable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;

@Mixin(Monster.class)
public abstract class MonsterMixin {
	@Inject(method = "isPreventingPlayerRest", at = @At("HEAD"), cancellable = true)
	private void isPreventingPlayerRest(ServerLevel server, Player player, CallbackInfoReturnable<Boolean> callback) {
		if (this instanceof Tamable tamable && tamable.isTame())
			callback.setReturnValue(false);
	}
}