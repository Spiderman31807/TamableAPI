package sxs.api.tamable.mixin;

import sxs.api.tamable.HasBabyModel;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.monster.hoglin.Hoglin;

@Mixin(Hoglin.class)
public abstract class HoglinMixin implements HasBabyModel {
}