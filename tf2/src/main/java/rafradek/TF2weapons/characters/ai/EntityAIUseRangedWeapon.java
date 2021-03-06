package rafradek.TF2weapons.characters.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;
import rafradek.TF2weapons.TF2Util;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.building.EntityBuilding;
import rafradek.TF2weapons.characters.EntityHeavy;
import rafradek.TF2weapons.characters.EntityTF2Character;
import rafradek.TF2weapons.message.TF2Message;
import rafradek.TF2weapons.weapons.ItemMedigun;
import rafradek.TF2weapons.weapons.ItemUsable;
import rafradek.TF2weapons.weapons.ItemWeapon;

public class EntityAIUseRangedWeapon extends EntityAIBase {

	protected final EntityTF2Character entityHost;

	protected EntityLivingBase attackTarget;
	public boolean reloading;

	protected int rangedAttackTime;
	protected float entityMoveSpeed;
	protected int comeCloser;

	private float attackRange;
	protected float attackRangeSquared;
	protected float attackRangeSSquared;
	
	protected boolean pressed;
	protected boolean dodging;
	public boolean jump;
	public float dodgeSpeed = 1f;
	public int jumprange;
	public float projSpeed;
	public double fireAtFeet;
	public double lastMotionY;

	public float gravity;

	public boolean explosive;

	public float releaseAt;
	public EntityAIUseRangedWeapon(EntityTF2Character par1IRangedAttackMob, float par2, float par5) {
		this.rangedAttackTime = -1;
		this.comeCloser = 0;
		this.entityHost = par1IRangedAttackMob;
		this.entityMoveSpeed = par2;
		this.attackRange = par5;
		this.attackRangeSquared = par5 * par5;

		this.setMutexBits(2);
	}

	public void setRange(float range) {
		this.attackRange = range;
		this.attackRangeSquared = range * range;
		this.attackRangeSSquared = (range+5) * (range+5);
	}

	/**
	 * Returns whether the EntityAIBase should begin execution.
	 */
	@Override
	public boolean shouldExecute() {
		EntityLivingBase EntityLivingBase = this.entityHost.getAttackTarget();

		if (EntityLivingBase == null)
			return false;
		else {
			if (this.attackTarget != EntityLivingBase)
				this.lastMotionY = EntityLivingBase.motionY;
			this.attackTarget = EntityLivingBase;
			return !this.entityHost.getHeldItemMainhand().isEmpty() && this.entityHost.eating <= 0
					&& (this.entityHost.getHeldItemMainhand().getItem() instanceof ItemWeapon || this.entityHost.getHeldItemMainhand().getItem() instanceof ItemMedigun);
		}
	}

	/**
	 * Returns whether an in-progress EntityAIBase should continue executing
	 */
	@Override
	public boolean shouldContinueExecuting() {
		return this.shouldExecute();
	}

	/**
	 * Resets the task
	 */
	@Override
	public void resetTask() {
		if ((this.entityHost.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemWeapon) &&this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state != 0) {
			pressed = false;
			((ItemWeapon) this.entityHost.getHeldItem(EnumHand.MAIN_HAND).getItem()).endUse(
					this.entityHost.getHeldItem(EnumHand.MAIN_HAND), this.entityHost, this.entityHost.world,
					this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state, 0);
			this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state = 0;
			TF2Util.sendTracking(new TF2Message.ActionMessage(0, entityHost), entityHost);
		}
		if (this.jump)
			this.entityHost.jump = false;
		this.attackTarget = null;
		this.comeCloser = 0;
		this.rangedAttackTime = -1;
	}

	@Override
	public void updateTask() {
		if ((this.attackTarget != null && this.attackTarget.deathTime > 0) || this.entityHost.deathTime > 0) {
			this.resetTask();
			return;
		}
		if (this.attackTarget == null)
			return;
		
		ItemStack item = this.entityHost.getHeldItem(EnumHand.MAIN_HAND);
		
		if (!(item.getItem() instanceof ItemWeapon))
			return;
		ItemWeapon weapon = ((ItemWeapon) item.getItem());

		double d0 = this.entityHost.getDistanceSq(this.attackTarget.posX, this.attackTarget.getEntityBoundingBox().minY,
				this.attackTarget.posZ);

		double lookX = this.attackTarget.posX;
		double lookY = this.attackTarget.posY + this.attackTarget.getEyeHeight();
		double lookZ = this.attackTarget.posZ;
		boolean shouldFireProj = true;
		float dist = this.entityHost.getDistanceToEntity(this.attackTarget);
		if (this.projSpeed > 0) {

			
			int ticksToReach = MathHelper.ceil(dist / this.projSpeed);

			lookX += (this.attackTarget.posX - this.entityHost.targetPrevPos[1]) * ticksToReach * 1;
			lookZ += (this.attackTarget.posZ - this.entityHost.targetPrevPos[5]) * ticksToReach * 1;
			lookY = this.attackTarget.posY + this.attackTarget.height / 2;
			if (this.fireAtFeet == -1 || this.entityHost.world.rayTraceBlocks(new Vec3d(this.entityHost.posX,
					this.entityHost.posY + this.entityHost.getEyeHeight(), this.entityHost.posZ),
					new Vec3d(lookX, lookY, lookZ), false, true, false) != null)
				lookY = this.attackTarget.posY + this.attackTarget.getEyeHeight();
			double yFall = !this.attackTarget.onGround? -this.attackTarget.motionY : 0;
			if (!this.attackTarget.isInWater()) {
				int ticksGrav = ticksToReach-1;
				int i = (ticksGrav * ticksGrav + ticksGrav)/2;
				lookY+= gravity * i;
				if (!this.attackTarget.onGround && this.attackTarget.motionY < 0)
					yFall += MathHelper.clamp(this.lastMotionY-this.attackTarget.motionY, 0, 0.1) * i;
				/*for (int i = 1; i <= ticksToReach; i++) {
					lookY += gravity * i;
					
						
				}*/
			}

			RayTraceResult mop = this.entityHost.world.rayTraceBlocks(this.attackTarget.getPositionVector(),
					this.attackTarget.getPositionVector().addVector(0, -0.3 - yFall, 0));
			if (mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK)
				yFall = this.attackTarget.posY - mop.hitVec.y;
			shouldFireProj = mop != null || this.attackTarget.motionY <= 0f;
			lookY -= yFall;
			
			if (this.fireAtFeet > 0 && this.entityHost.world.rayTraceBlocks(
					new Vec3d(this.entityHost.posX, this.entityHost.posY + this.entityHost.getEyeHeight(),
							this.entityHost.posZ),
					new Vec3d(lookX, this.attackTarget.posY, lookZ), false, true, false) == null) {
				lookY -= (this.attackTarget.height/2)*this.fireAtFeet;
				
			}
			//System.out.println("look: "+(this.lastMotionY-this.attackTarget.motionY));

			/*RayTraceResult ray = this.entityHost.rayTrace(128d, 1f);
			if (ray != null)
				((WorldServer)this.entityHost.world).spawnParticle(EnumParticleTypes.CRIT, ray.hitVec.x, ray.hitVec.y, ray.hitVec.z,1,0, 0, 0, 0, new int[0]);*/
		}

		this.lastMotionY = this.attackTarget.motionY;
		boolean charged = weapon.holdingMode(item, entityHost) > 0 && weapon.getCharge(entityHost, item) >= this.releaseAt;
		//System.out.println("charge: "+weapon.getCharge(entityHost, item));
		boolean stay = this.entityHost.getEntitySenses().canSee(this.attackTarget)
				|| (this.projSpeed > 0 && this.attackTarget.motionY > 0);
		
		boolean fire = stay && this.entityHost.getActivePotionEffect(TF2weapons.stun) == null && shouldFireProj && !charged && TF2Util.lookingAt(this.entityHost,
				(this.explosive && d0 < 16 ? 30 : 0) + weapon.getWeaponSpreadBase(item, this.entityHost) * (this.projSpeed > 0 ? 100 : 1) + 2 + Math.toDegrees(MathHelper.atan2(this.attackTarget.width / 2, dist)),
				lookX, lookY, lookZ);
		
		if (!this.reloading && (this.entityHost.world.getDifficulty() != EnumDifficulty.HARD || !fire)
				&& item.getItemDamage() == item.getMaxDamage() && weapon.hasClip(item))
			this.reloading = true;
		else if (this.reloading && item.getItemDamage() == 0)
			this.reloading = false;
		
		this.entityHost.getLookHelper().setLookPosition(lookX, lookY, lookZ, this.entityHost.rotation, 90.0F);
		
		if ((!reloading) && fire && d0 <= (this.attackTarget instanceof EntityBuilding ? this.attackRangeSSquared : this.attackRangeSquared)
				&& (((ItemUsable)item.getItem()).isAmmoSufficient(item, entityHost, true) || weapon.getAmmoType(item) == 0)) {
			this.reloading = false;
			if (!pressed) {
				pressed = true;
				weapon.startUse(item, this.entityHost, this.entityHost.world,
						this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state, 1);
				this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state = 1;
				TF2Util.sendTracking(new TF2Message.ActionMessage(1, entityHost), entityHost);
			}

		} else if (weapon.getCharge(entityHost, item) >= this.releaseAt){
			if (pressed) {
				if (this.jump)
					this.entityHost.jump = false;
				int valuedef = this.entityHost instanceof EntityHeavy ? 2 : 0;
				weapon.endUse(item, this.entityHost, this.entityHost.world,
						this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state, valuedef);
				this.entityHost.getCapability(TF2weapons.WEAPONS_CAP, null).state = valuedef;
				TF2Util.sendTracking(new TF2Message.ActionMessage(valuedef, entityHost),
						entityHost);
			}
			pressed = false;
		}
	}

}
