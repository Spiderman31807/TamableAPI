package sxs.api.tamable.mixin;

import sxs.api.tamable.HasBabyModel;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.monster.piglin.Piglin;

@Mixin(Piglin.class)
public abstract class PiglinMixin implements HasBabyModel {
}