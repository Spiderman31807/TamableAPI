package sxs.api.tamable;

import sxs.api.attachments.Attachments;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;

public interface Collarable {
	default InteractionResult collarInteract(Player player, ItemStack itemStack, DyeItem dye) {
		DyeColor color = dye.getDyeColor();
		if (color == this.getCollarColor())
			return InteractionResult.PASS;
		this.setCollarColor(color);
		itemStack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	default DyeColor getCollarColor() {
		return Attachments.orElse(this, Events.Color, DyeColor.RED);
	}

	default void setCollarColor(DyeColor dye) {
		Attachments.set(this, Events.Color, dye);
	}
}