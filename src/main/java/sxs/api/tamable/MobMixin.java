package sxs.api.tamable.mixin;

import sxs.api.attachments.Attachments;

import sxs.api.tamable.Events;
import sxs.api.tamable.Tamable;
import sxs.api.tamable.Collarable;
import sxs.api.tamable.Breedable;
import sxs.api.tamable.Ageable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.world.level.entity.UUIDLookup;
import net.minecraft.core.particles.ParticleTypes;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements Tamable {
	@Unique
    private boolean sitting = false;
	@Unique
    private int inLove = 0;
	@Unique
    private EntityReference<ServerPlayer> loveCause;
	@Unique
    private int age = 0;
	@Unique
    private int forcedAge = 0;
	@Unique
    private int forcedAgeTimer;
    
	private MobMixin() {
		super(null, null);
	}

	public boolean isSitting() {
		return this.sitting;
	}

	public void setSitting(boolean sitting) {
		this.sitting = sitting;
	}

	public void setInLoveTime(int time) {
        this.inLove = time;
    }

	public int getInLoveTime() {
        return this.inLove;
    }

	public void setLoveCause(EntityReference<ServerPlayer> cause) {
		this.loveCause = cause;
	}

    public ServerPlayer getLoveCause() {
        return EntityReference.get(this.loveCause, (UUIDLookup<Player>) this.level()::getPlayerByUUID, ServerPlayer.class);
    }

	public void setAgeD(int age) {
		this.age = age;
	}

	public int getAge() {
		if (this.level().isClientSide)
			return Attachments.orElse(this, Events.IsBaby, false) ? -1 : 1;
		else
			return this.age;
	}

    public void addForcedAge(int amount) {
    	this.forcedAge += amount;
    	if (this.forcedAgeTimer == 0)
        	this.forcedAgeTimer = 40;
    }

    public int getForcedAge() {
        return this.forcedAge;
    }

    public int getForcedAgeTimer() {
        return this.forcedAgeTimer;
    }

	@Override
	public boolean canAttack(LivingEntity target) {
		return this instanceof Tamable tamable && tamable.isOwnedBy(target) ? false : super.canAttack(target);
	}

	@Override
	public PlayerTeam getTeam() {
		PlayerTeam team = super.getTeam();
		if (team != null)
			return team;
			
		if (this instanceof Tamable tamable && tamable.isTame()) {
			LivingEntity owner = tamable.getRootOwner();
			if (owner != null)
				return owner.getTeam();
		}
		
		return null;
	}

	@Override
	protected boolean considersEntityAsAlly(Entity entity) {
		if (this instanceof Tamable tamable && tamable.isTame()) {
			LivingEntity owner = tamable.getRootOwner();
			if (entity == owner)
				return true;
			if (owner != null)
				return owner.isAlliedTo(entity);
		}
		
		return super.considersEntityAsAlly(entity);
	}

	@Override
	public void die(DamageSource source) {
		Component message = this.getCombatTracker().getDeathMessage();
		super.die(source);
		if (this instanceof Tamable tamable && this.dead && this.level() instanceof ServerLevel serverLevel && serverLevel.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && tamable.getOwner() instanceof ServerPlayer owner)
			owner.sendSystemMessage(message);
	}
	
    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
    	if(this instanceof Tamable tamable) {
	        if (this.isInvulnerableTo(serverLevel, source))
	            return false;
	        tamable.setSitting(false);
    	}
    	
        return super.hurtServer(serverLevel, source, amount);
    }
    
	@Override
    protected void actuallyHurt(ServerLevel serverLevel, DamageSource source, float amount) {
    	if(this instanceof Breedable breedable)
       		breedable.resetLove();
        super.actuallyHurt(serverLevel, source, amount);
    }

    @Override
    public boolean isBaby() {
        return this instanceof Ageable ageable ? ageable.getAge() < 0 : super.isBaby();
    }

	@Inject(method = "setBaby", at = @At("HEAD"))
    private void setBaby(boolean isBaby, CallbackInfo callback) {
    	if(this instanceof Ageable ageable)
        	ageable.setAge(isBaby ? -24000 : 0);
    }

	@Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void customServerAiStep(ServerLevel serverLevel, CallbackInfo callback) {
        if (this instanceof Breedable breedable && breedable.getAge() != 0)
            this.inLove = 0;
    }

	@Inject(method = "aiStep", at = @At("TAIL"))
    private void aiStep(CallbackInfo callback) {
		if(this instanceof Ageable ageable) {
	        if (this.level().isClientSide) {
	            if (this.forcedAgeTimer > 0) {
	                if (this.forcedAgeTimer % 4 == 0)
	                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1), this.getRandomY() + 0.5, this.getRandomZ(1), 0, 0, 0);
	                this.forcedAgeTimer--;
	            }
	        } else if (this.isAlive()) {
	            int age = ageable.getAge();
	            if (age < 0)
	                ageable.setAge(++age);
	            else if (age > 0)
	                ageable.setAge(--age);
	        }
		}
    
        if (this instanceof Breedable breedable) {
	        if (breedable.getAge() != 0)
	            this.inLove = 0;
	            
	        if (this.inLove > 0) {
	            this.inLove--;
	            if (this.inLove % 10 == 0) {
	                double deltaX = this.random.nextGaussian() * 0.02;
	                double deltaY = this.random.nextGaussian() * 0.02;
	                double deltaZ = this.random.nextGaussian() * 0.02;
	                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1), this.getRandomY() + 0.5, this.getRandomZ(1), deltaX, deltaY, deltaZ);
	            }
	        }
        }
    }
    
	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void mobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
		if(this instanceof Breedable breedable) {
			InteractionResult result = breedable.breedInteract(player, hand);
			if(result != InteractionResult.PASS) {
				callback.setReturnValue(result);
				return;
			}
		}
		
		if(this instanceof Tamable tamable) {
			InteractionResult result = tamable.tamableInteract(player, hand);
			if(result != InteractionResult.PASS) {
				callback.setReturnValue(result);
				return;
			}
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void addAdditionalSaveData(ValueOutput output, CallbackInfo callback) {
		if (this instanceof Tamable tamable) {
			output.putBoolean("Sitting", tamable.isSitting());
			EntityReference<LivingEntity> owner = tamable.getOwnerReference();
			EntityReference.store(owner, output, "Owner");
			if (this instanceof Collarable collarable)
				output.store("CollarColor", DyeColor.LEGACY_ID_CODEC, collarable.getCollarColor());
		}

		if(this instanceof Breedable breedable) {
	        output.putInt("InLove", this.inLove);
	        EntityReference.store(this.loveCause, output, "LoveCause");
		}

		if(this instanceof Ageable ageable) {
	        output.putInt("Age", ageable.getAge());
	        output.putInt("ForcedAge", this.forcedAge);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditionalSaveData(ValueInput input, CallbackInfo callback) {
		if (this instanceof Tamable tamable) {
	        EntityReference<LivingEntity> owner = EntityReference.readWithOldOwnerConversion(input, "Owner", this.level());
	        if (owner != null) {
	            try {
	            	tamable.setOwnerReference(owner);
	                tamable.setTame(true, false);
	            } catch (Throwable throwable) {
	                tamable.setTame(false, true);
	            }
	        } else {
	            tamable.setOwnerReference(null);
	            tamable.setTame(false, true);
	        }
	
	        tamable.setSitting(input.getBooleanOr("Sitting", false));
	        tamable.setInSittingPose(tamable.isSitting());
	        
			if(this instanceof Collarable collarable)
        		collarable.setCollarColor(input.read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DyeColor.RED));
		}

		if(this instanceof Breedable breedable) {
	        this.inLove = input.getIntOr("InLove", 0);
	        this.loveCause = EntityReference.read(input, "LoveCause");
		}

		if(this instanceof Ageable ageable) {
	        ageable.setAge(input.getIntOr("Age", 0));
	        this.forcedAge = input.getIntOr("ForcedAge", 0);
		}
    }

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	public void handleEntityEvent(byte event, CallbackInfo callback) {
		if ((event == 6 || event == 7) && this instanceof Tamable tamable) {
			if (event == 7)
				tamable.spawnTamingParticles(true);
			else if (event == 6)
				tamable.spawnTamingParticles(false);
			callback.cancel();
		} else if(event == 18 && this instanceof Breedable breedable) {
	        for (int i = 0; i < 7; i++) {
	            double deltaX = this.random.nextGaussian() * 0.02;
	            double deltaY = this.random.nextGaussian() * 0.02;
	            double deltaZ = this.random.nextGaussian() * 0.02;
		        this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1), this.getRandomY() + 0.5, this.getRandomZ(1), deltaX, deltaY, deltaZ);
	    	}
	    	
			callback.cancel();
		}
	}
	
	@Inject(method = "getAmbientSoundInterval", at = @At("HEAD"), cancellable = true)
    private void getAmbientSoundInterval(CallbackInfoReturnable<Integer> callback) {
		if (this instanceof Tamable tamable && tamable.isTame())
			callback.setReturnValue(120);
    }
	
	@Inject(method = "removeWhenFarAway", at = @At("HEAD"), cancellable = true)
    private void removeWhenFarAway(double distance, CallbackInfoReturnable<Boolean> callback) {
		if (this instanceof Tamable tamable && tamable.isTame())
			callback.setReturnValue(false);
    }

	@Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
	private void canBeLeashed(CallbackInfoReturnable<Boolean> callback) {
		if (this instanceof Tamable tamable && tamable.isTame())
			callback.setReturnValue(true);
	}
}