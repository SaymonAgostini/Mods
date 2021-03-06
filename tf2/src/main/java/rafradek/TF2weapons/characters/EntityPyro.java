package rafradek.TF2weapons.characters;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import rafradek.TF2weapons.ItemFromData;
import rafradek.TF2weapons.TF2Sounds;
import rafradek.TF2weapons.TF2Util;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.WeaponData.PropertyType;
import rafradek.TF2weapons.characters.ai.EntityAIAirblast;
import rafradek.TF2weapons.weapons.ItemBackpack;
import rafradek.TF2weapons.weapons.ItemFlameThrower;
import rafradek.TF2weapons.weapons.ItemHorn;
import rafradek.TF2weapons.weapons.ItemJetpack;
import rafradek.TF2weapons.weapons.ItemProjectileWeapon;
import rafradek.TF2weapons.weapons.ItemSoldierBackpack;

public class EntityPyro extends EntityTF2Character {

	public EntityPyro(World par1World) {
		super(par1World);
		this.tasks.addTask(3, new EntityAIAirblast(this));
		if (this.attack != null) {
			this.moveAttack.setDodge(true, true);
			this.attack.dodgeSpeed = 1.2f;
			this.attack.setRange(6.2865F);
			this.attack.projSpeed = 1.2570f;
		}
		this.rotation = 16;
		//this.ammoLeft = 250;
		this.experienceValue = 15;
		// ((PathNavigateGround)this.getNavigator()).set(true);
		// this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
		// ItemUsable.getNewStack("Minigun"));

	}

	@Override
	protected ResourceLocation getLootTable() {
		return TF2weapons.lootPyro;
	}

	public float[] getDropChance() {
		return new float[] { 0.062f, 0.12f, 0.11f };
	}
	
	protected void addWeapons() {
		super.addWeapons();
		if(this.loadout.getStackInSlot(1).getItem() instanceof ItemJetpack) {
			this.setItemStackToSlot(EntityEquipmentSlot.CHEST, this.loadout.getStackInSlot(1));
		}
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source == DamageSource.ON_FIRE)
			return false;
		return super.attackEntityFrom(source, amount);
	}

	@Override
	public void setFire(int time) {
		super.setFire(1);
	}

	/*
	 * protected void addWeapons() {
	 * this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND,
	 * ItemFromData.getNewStack("flamethrower")); }
	 */
	@Override
	public void onLivingUpdate() {

		if(!this.world.isRemote && this.getAttackTarget() != null){
			if(this.getDiff()>1 && this.loadout.getStackInSlot(1).getItem() instanceof ItemProjectileWeapon){
				if(this.usedSlot==0 && this.getDistanceSqToEntity(this.getAttackTarget())>64){
					//System.out.println("Shotgun switch");
					this.switchSlot(1);
					//this.ammoLeft++;
				}
				else if(this.usedSlot==1 && this.getDistanceSqToEntity(this.getAttackTarget())<44){
					this.switchSlot(0);
				}
			}
			if (this.getWepCapability().getPhlogRage() >= 20f 
					&& this.getHeldItemMainhand().getItem() instanceof ItemFlameThrower && !this.getHeldItemMainhand().getTagCompound().getBoolean("RageActive")) {
				this.addPotionEffect(new PotionEffect(TF2weapons.stun,40,1));
				this.addPotionEffect(new PotionEffect(TF2weapons.noKnockback,40,0));
				TF2Util.addAndSendEffect(this, new PotionEffect(TF2weapons.uber,40,0));
				this.getHeldItemMainhand().getTagCompound().setBoolean("RageActive", true);
				this.playSound(ItemFromData.getSound(this.getHeldItemMainhand(), PropertyType.CHARGE_SOUND), this.getSoundVolume(), this.getSoundPitch());
			}
			
			ItemStack backpack = this.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
			if (!this.world.isRemote && backpack.getItem() instanceof ItemJetpack) {
				if (this.getAttackTarget() != null && this.getDistanceSqToEntity(this.getAttackTarget()) > 120 && ((ItemJetpack)backpack.getItem()).canActivate(backpack, this)) {
					((ItemJetpack)backpack.getItem()).activateJetpack(backpack, this, true);
					
				}
			}
		}
		super.onLivingUpdate();
	}
	
	
	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(35.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(18.5D);
		this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.1329D);
		this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
	}

	/*
	 * public void onLivingUpdate() { super.onLivingUpdate();
	 * if(this.ammoLeft>0&&this.getAttackTarget()!=null&&this.
	 * getDistanceSqToEntity(this.getAttackTarget())<=400&&(!TF2ActionHandler.
	 * playerAction.get(this.world.isRemote).containsKey(this)||(
	 * TF2ActionHandler.playerAction.get(this.world.isRemote).get(this)&3)==0
	 * )){ TF2ActionHandler.playerAction.get(this.world.isRemote).put(this,
	 * TF2ActionHandler.playerAction.get(this.world.isRemote).containsKey(
	 * this)?TF2ActionHandler.playerAction.get(this.world.isRemote).get(this)
	 * +2:2); } }
	 */
	@Override
	protected SoundEvent getAmbientSound() {
		return TF2Sounds.MOB_PYRO_SAY;
	}

	/**
	 * Returns the sound this mob makes when it is hurt.
	 */
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return TF2Sounds.MOB_PYRO_HURT;
	}

	/**
	 * Returns the sound this mob makes on death.
	 */
	@Override
	protected SoundEvent getDeathSound() {
		return TF2Sounds.MOB_PYRO_DEATH;
	}

	/**
	 * Get this Entity's EnumCreatureAttribute
	 */
	@Override
	protected void dropFewItems(boolean p_70628_1_, int p_70628_2_) {
		if (this.rand.nextFloat() < 0.15f + p_70628_2_ * 0.075f)
			this.entityDropItem(ItemFromData.getNewStack("shotgun"), 0);
		if (this.rand.nextFloat() < 0.05f + p_70628_2_ * 0.025f)
			this.entityDropItem(ItemFromData.getNewStack("flamethrower"), 0);
	}
	@Override
	public float getAttributeModifier(String attribute) {
		if (shouldScaleAttributes())
			if (attribute.equals("Damage"))
				return 0.93f;
		return super.getAttributeModifier(attribute);
	}
	/*
	 * @Override public float getAttributeModifier(String attribute) {
	 * if(attribute.equals("Minigun Spinup")){ return
	 * super.getAttributeModifier(attribute)*1.5f; } return
	 * super.getAttributeModifier(attribute); }
	 */
	public int getClassIndex() {
		return 2;
	}
}
