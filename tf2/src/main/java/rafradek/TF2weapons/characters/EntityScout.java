package rafradek.TF2weapons.characters;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import rafradek.TF2weapons.ItemFromData;
import rafradek.TF2weapons.TF2Attribute;
import rafradek.TF2weapons.TF2Sounds;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.weapons.ItemCleaver;
import rafradek.TF2weapons.weapons.ItemWeapon;

public class EntityScout extends EntityTF2Character {
	public boolean doubleJumped;
	private int jumpDelay;

	public int ballCooldown;
	public EntityScout(World par1World) {
		super(par1World);
		if (this.attack != null) {
			this.attack.setDodge(true, true);
			this.attack.jump = true;
			this.attack.jumprange = 40;
			this.attack.dodgeSpeed = 1.25f;
		}
		this.ammoLeft = 24;
		this.experienceValue = 15;

		// this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
		// ItemUsable.getNewStack("Minigun"));

	}

	/*
	 * protected void addWeapons() {
	 * this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
	 * ItemFromData.getNewStack("scattergun")); }
	 */
	@Override
	protected void addWeapons() {
		super.addWeapons();
		if(TF2Attribute.getModifier("Crit Stun", this.loadout.get(1), 0, this) != 0) {
			this.loadout.set(2, ItemFromData.getNewStack("sandmanball"));
		}
	}
	
	@Override
	protected ResourceLocation getLootTable() {
		return TF2weapons.lootScout;
	}
	
	public float[] getDropChance() {
		return new float[] { 0.1f, 0.12f, 0.11f };
	}
	
	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(15.0D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.364D);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		
		if (jumpDelay > 0 && --jumpDelay == 0 && (this.onGround || !this.doubleJumped))
			this.jump();
		if (this.onGround)
			this.doubleJumped = false;

		if(!this.world.isRemote && TF2Attribute.getModifier("Crit Stun", this.loadout.get(1), 0, this) != 0) {
			this.ballCooldown--;
			if(this.getAttackTarget() == null || this.getAttackTarget().getActivePotionEffect(TF2weapons.stun) == null) {
				this.switchSlot(2);
				if(this.getAttackTarget() != null && this.getWepCapability().fire1Cool<=0 && this.getEntitySenses().canSee(this.getAttackTarget())) {
					((ItemWeapon)this.getHeldItemMainhand().getItem()).altUse(getHeldItemMainhand(), this, world);
					this.getWepCapability().fire1Cool=1000;
				}
			}
			else {
				this.switchSlot(1);
				this.getHeldItemMainhand().setCount(16);
			}
		}
	}

	@Override
	protected void jump() {
		super.jump();
		/*
		 * double speed=Math.sqrt(motionX*motionX+motionZ*motionZ);
		 * if(speed!=0){
		 * 
		 * double speedMultiply=this.getEntityAttribute(SharedMonsterAttributes.
		 * MOVEMENT_SPEED).getAttributeValue()/speed;
		 * this.motionX*=speedMultiply; this.motionZ*=speedMultiply; }
		 */
		this.motionX = (-MathHelper.sin(this.rotationYaw / 180.0F * (float) Math.PI));
		this.motionZ = (MathHelper.cos(this.rotationYaw / 180.0F * (float) Math.PI));
		float f2 = (float) (MathHelper.sqrt(motionX * motionX + motionZ * motionZ)
				* this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
		this.motionX *= f2;
		this.motionZ *= f2;
		this.fallDistance = -3.0F;
		if (!this.doubleJumped && this.jump) {
			this.doubleJumped = true;
			this.jumpDelay = 8;
		}
	}

	public float getAttributeModifier(String attribute) {
		if (this.loadout.get(1).getItem() instanceof ItemCleaver)
			if (attribute.equals("Fire Rate"))
				return this.getDiff() == 1 ? 1.8f : (this.getDiff() == 3 ? 1f : 1.3f);
		return super.getAttributeModifier(attribute);
	}
	
	@Override
	protected SoundEvent getAmbientSound() {
		return TF2Sounds.MOB_SCOUT_SAY;
	}

	/**
	 * Returns the sound this mob makes when it is hurt.
	 */
	@Override
	protected SoundEvent getHurtSound() {
		return TF2Sounds.MOB_SCOUT_HURT;
	}

	/**
	 * Returns the sound this mob makes on death.
	 */
	@Override
	protected SoundEvent getDeathSound() {
		return TF2Sounds.MOB_SCOUT_DEATH;
	}

	/*
	 * public int getAttackStrength(Entity par1Entity) { ItemStack itemstack =
	 * this.getHeldItem(EnumHand.MAIN_HAND); float f =
	 * (float)(this.getMaxHealth() - this.getHealth()) /
	 * (float)this.getMaxHealth(); int i = 4 + MathHelper.floor_float(f * 4.0F);
	 * 
	 * if (item!stack.isEmpty()) { i += itemstack.getDamageVsEntity(this); }
	 * 
	 * return i; }
	 */

	/**
	 * Plays step sound at given x, y, z for the entity
	 */
	@Override
	protected void dropFewItems(boolean p_70628_1_, int p_70628_2_) {
		if (this.rand.nextFloat() < 0.60f + p_70628_2_ * 0.30f)
			this.entityDropItem(ItemFromData.getNewStack("bonk"), 0);
		if (this.rand.nextFloat() < 0.12f + p_70628_2_ * 0.075f)
			this.entityDropItem(ItemFromData.getNewStack("scattergun"), 0);
		if (this.rand.nextFloat() < 0.15f + p_70628_2_ * 0.075f)
			this.entityDropItem(ItemFromData.getNewStack("pistol"), 0);
	}

	@Override
	public float getMotionSensitivity() {
		return 0;
	}
	
	@Override
	public void onShot() {
		if (ItemFromData.getData(this.loadout.get(2)).getName().equals("sandmanball"))
			this.ballCooldown = this.getDiff() == 1 ? 340 : (this.getDiff() == 3 ? 160 : 240);
	}
}
