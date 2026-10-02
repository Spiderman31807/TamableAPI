package sxs.api.tamable.mixin;

import sxs.api.tamable.HasBabyModel;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.monster.Zoglin;

@Mixin(Zoglin.class)
public abstract class ZoglinMixin implements HasBabyModel {
}