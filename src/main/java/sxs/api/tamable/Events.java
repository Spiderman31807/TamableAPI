package sxs.api.tamable;

import sxs.injector.goals.DefineGoalsEvent;
import sxs.api.attachments.RegisterAttachmentsEvent;
import sxs.api.attachments.UpdateAttachmentEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

@EventBusSubscriber
public class Events {
	public static final ResourceLocation Tamed = ResourceLocation.fromNamespaceAndPath("tamable_api", "tamed");
	public static final ResourceLocation Sitting = ResourceLocation.fromNamespaceAndPath("tamable_api", "sitting");
	public static final ResourceLocation Owner = ResourceLocation.fromNamespaceAndPath("tamable_api", "owner");
	public static final ResourceLocation Color = ResourceLocation.fromNamespaceAndPath("tamable_api", "color");
	public static final ResourceLocation IsBaby = ResourceLocation.fromNamespaceAndPath("tamable_api", "is_baby");

	@SubscribeEvent
	public static void registerAttachments(RegisterAttachmentsEvent event) {
		if(event.getEntity() instanceof Tamable) {
			event.register(Boolean.class, Tamed, false, true);
			event.register(Boolean.class, Sitting, false, true);
			event.register(EntityReference.class, Owner, () ->  null, EntityReference.<LivingEntity>streamCodec());
		}
		
		if(event.getEntity() instanceof Collarable)
			event.register(DyeColor.class, Color, () -> DyeColor.RED, false, true);
			
		if(event.getEntity() instanceof Ageable)
			event.register(Boolean.class, IsBaby, false, true);
	}

	@SubscribeEvent
	public static void registerAttachments(UpdateAttachmentEvent.Post<Boolean> event) {
		if(event.is(IsBaby) && event.getHolder() instanceof LivingEntity entity)
			entity.refreshDimensions();
	}

	@SubscribeEvent
	public static void defineGoals(DefineGoalsEvent event) {
		if(event.getMob() instanceof Breedable breedable)
			event.insertBeforeGoal(new BreedGoal((Mob & Breedable) breedable), (goal) -> goal instanceof RandomStrollGoal);
			
		if (event.getMob() instanceof Tamable tamable) {
			SitWhenOrderedToGoal sitGoal = new SitWhenOrderedToGoal((Mob & Tamable) tamable);
			if(!event.insertAfterGoal(sitGoal, (goal) -> goal instanceof FloatGoal))
				event.addGoal(1, sitGoal);

			if(tamable instanceof PathfinderMob) {
				event.insertBeforeGoal(new TamablePanicGoal((PathfinderMob & Tamable) tamable), (goal) -> goal == sitGoal);
				event.insertBeforeGoal(new FollowOwnerGoal((PathfinderMob & Tamable) tamable), (goal) -> goal instanceof RandomStrollGoal);
			}
			
			OwnerHurtTargetGoal hurtGoal = new OwnerHurtTargetGoal((Mob & Tamable) tamable);
			if (!event.insertBeforeGoal(hurtGoal, (goal) -> goal instanceof HurtByTargetGoal))
				event.addGoal(1, hurtGoal);
				
			event.insertAfterGoal(new OwnerHurtByTargetGoal((Mob & Tamable) tamable), (goal) -> goal == hurtGoal);
		}
	}

	static void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
		int i = stack.getCount();
		UseRemainder remainder = stack.get(DataComponents.USE_REMAINDER);
		stack.consume(1, player);
		if (remainder != null) {
			ItemStack itemstack = remainder.convertIntoRemainder(stack, i, player.hasInfiniteMaterials(), player::handleExtraItemsCreatedOnUse);
			player.setItemInHand(hand, itemstack);
		}
	}
}