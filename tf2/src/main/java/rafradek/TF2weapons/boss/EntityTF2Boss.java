package rafradek.TF2weapons.boss;

import java.util.HashSet;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import rafradek.TF2weapons.ItemFromData;
import rafradek.TF2weapons.TF2Attribute;
import rafradek.TF2weapons.TF2ConfigVars;
import rafradek.TF2weapons.TF2DamageSource;
import rafradek.TF2weapons.TF2Sounds;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.characters.IEntityTF2;
import rafradek.TF2weapons.weapons.ItemMinigun;

public abstract class EntityTF2Boss extends EntityMob implements IEntityTF2 {

	protected final BossInfoServer bossInfo = (new BossInfoServer(this.getDisplayName(), BossInfo.Color.PURPLE,
				BossInfo.Overlay.PROGRESS));
	public int level = 1;
	public int timeLeft = 2400;
	public HashSet<EntityPlayer> attackers = new HashSet<EntityPlayer>();
	public int playersAttacked = 0;
	private int blockBreakCounter=27;
	public BlockPos spawnPos;

	public float damageMult=1;
	public EntityTF2Boss(World worldIn) {
		super(worldIn);
		if(!this.world.isRemote)
			this.setGlowing(true);
		this.inventoryHandsDropChances=new float[]{0,0};
	}
	
	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source == DamageSource.DROWN || source == DamageSource.LAVA || source == DamageSource.ON_FIRE)
			return false;
		if (source instanceof TF2DamageSource) {
			if (source.getTrueSource()==this)
				return false;
			if (((TF2DamageSource) source).getCritical() > 0) {
				amount *= 0.7f;
			}
			if (!((TF2DamageSource) source).getWeapon().isEmpty()
					&& ((TF2DamageSource) source).getWeapon().getItem() instanceof ItemMinigun)
				amount *= 0.36f;
		}
		if (super.attackEntityFrom(source, amount*damageMult)) {
			if (source.getTrueSource() != null && source.getTrueSource() instanceof EntityPlayer)
				this.attackers.add((EntityPlayer) source.getTrueSource());

			return true;
		}
		return false;
	}
	@Override
	public void fall(float distance, float damageMultiplier) {
		super.fall(distance, 0);
	}
	public void applyEntityCollision(Entity entityIn)
    {
		
    }
	@Override
	protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) {
		int count=this.playersAttacked;
		if(count>4){
			count=2+this.playersAttacked/2;
		}
		for(int i=0;i<count;i++){
			if(i>0)
				this.dropFewItems(wasRecentlyHit, lootingModifier);
			this.entityDropItem(new ItemStack(TF2weapons.itemTF2,MathHelper.log2(this.level)+1,2), 0);
			ItemStack weapon=ItemFromData.getRandomWeapon(this.rand, ItemFromData.VISIBLE_WEAPON);
			if(this.level>2)
				TF2Attribute.upgradeItemStack(weapon,40+(this.level-3)*55+MathHelper.log2(this.level)*40, rand);
			this.entityDropItem(weapon, 0);
		}
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		this.timeLeft--;
		if (!this.world.isRemote) {
			if (this.getAttackTarget() != null && !this.getAttackTarget().isEntityAlive()){
				this.setAttackTarget(null);
			}
			if (timeLeft==2250)
				this.setGlowing(false);
			if (timeLeft == 1200)
				this.playSound(TF2Sounds.MOB_BOSS_ESCAPE_60, 4F, 1f);
			else if (timeLeft == 200)
				this.playSound(TF2Sounds.MOB_BOSS_ESCAPE_10, 4F, 1f);
			else if (timeLeft <= 0){
				this.playSound(TF2Sounds.MOB_BOSS_ESCAPE, 4F, 1f);
				this.setDead();
			}
			this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
			--this.blockBreakCounter;

			if (this.blockBreakCounter <= 0 && this.world.getGameRules().getBoolean("mobGriefing")) {
				this.blockBreakCounter = 27;
				breakBlocks();
			}
		}

	}
	public boolean breakBlocks(){
		boolean flag = false;
		AxisAlignedBB box=this.getBreakingBB();
		for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); ++x)
			for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); ++y)
				for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); ++z) {
					BlockPos blockpos = new BlockPos(x, y, z);
					IBlockState iblockstate = this.world.getBlockState(blockpos);
					Block block = iblockstate.getBlock();

					if (!block.isAir(iblockstate, this.world, blockpos) && !iblockstate.getMaterial().isLiquid()
							&& EntityWither.canDestroyBlock(block)
							&& block.canEntityDestroy(iblockstate, world, blockpos, this))
						flag = this.world.destroyBlock(blockpos, true) || flag;
				}

		if (flag)
			this.world.playEvent((EntityPlayer) null, 1022, new BlockPos(this), 0);
		return flag;
	}
	public float getSoundVolume(){
		return TF2ConfigVars.bossVolume;
	}
	public AxisAlignedBB getBreakingBB(){
		return this.getEntityBoundingBox();
	}
	@Override
	public boolean isNonBoss() {
		return false;
	}
	public SoundEvent getAppearSound(){
		return null;
	}
	@Override
	public Team getTeam() {
		return this.world.getScoreboard().getTeam("TF2Bosses");
	}

	@Override
	public void setFire(int time) {
		super.setFire(1);
	}

	@Override
	public IEntityLivingData onInitialSpawn(DifficultyInstance diff, IEntityLivingData p_110161_1_) {
		int players = 0;
		int highestLevel = 0;
		int statmult=0;
		this.spawnPos = this.getPosition();
		this.setHomePosAndDistance(this.getPosition(), 40);
		for (EntityLivingBase living : this.world.getEntitiesWithinAABB(EntityLivingBase.class,
				this.getEntityBoundingBox().grow(64, 64, 64),new Predicate<EntityLivingBase>(){

					@Override
					public boolean apply(EntityLivingBase input) {
						// TODO Auto-generated method stub
						return input.hasCapability(TF2weapons.WEAPONS_CAP, null);
					}
			
		})) {
			
			statmult++;
			if(living instanceof EntityPlayer){
				players++;
				statmult+=2;
				EntityPlayer player=(EntityPlayer) living;
				if(player.getCapability(TF2weapons.PLAYER_CAP, null).highestBossLevel.get(this.getClass())==null){
					player.getCapability(TF2weapons.PLAYER_CAP, null).highestBossLevel.put(this.getClass(), (short)0);
				}
				int level = player.getCapability(TF2weapons.PLAYER_CAP, null)
						.highestBossLevel
						.get(this.getClass());
				if (level > highestLevel)
					highestLevel = level;
				player.sendMessage(new TextComponentTranslation("tf2boss.appear",new Object[] {this.getDisplayName(),Math.min(30, highestLevel+1)}));
			}
		}
		highestLevel++;
		this.level = Math.min(30, highestLevel);
		//System.out.println("Level: " + this.level + " player: " + players);
		float desiredHealth=(float)this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() * (0.6f + statmult * 0.13333f) * (0.6f + highestLevel * 0.4f);
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
				.setBaseValue(Math.min(1000,desiredHealth));
		this.damageMult=Math.min(1f, 1000f/desiredHealth);
		this.setHealth(this.getMaxHealth());
		TF2Attribute.setAttribute(this.getHeldItemMainhand(), TF2Attribute.attributes[19],
				1 * (0.85f + this.level * 0.15f));
		this.experienceValue = (int) (200 * (0.5f + players * 0.5f) * (0.45f + highestLevel * 0.55f));
		this.playersAttacked=players;
		this.playSound(this.getAppearSound(), 4F, 1);
		return p_110161_1_;
	}

	@Override
	public void onDeath(DamageSource cause) {
		super.onDeath(cause);
		for (EntityPlayer player : this.attackers) {
			int level = player.getCapability(TF2weapons.PLAYER_CAP, null).highestBossLevel.get(this.getClass()) != null
					? player.getCapability(TF2weapons.PLAYER_CAP, null).highestBossLevel.get(this.getClass()) : 0;
			if (this.level > level)
				player.getCapability(TF2weapons.PLAYER_CAP, null).highestBossLevel.put(this.getClass(),
						(short) this.level);
			player.sendMessage(new TextComponentTranslation("tf2boss.death",new Object[] {this.getDisplayName(),this.level}));
		}
	}
	/*public void addAchievement(EntityPlayer player){
		if(this.level>=30)
			player.addStat(TF2Achievements.BOSS_30_LVL);
	}*/
	@Override
	public void writeEntityToNBT(NBTTagCompound nbt) {
		super.writeEntityToNBT(nbt);
		nbt.setShort("Level", (short)this.level);
		nbt.setShort("Players", (short) this.playersAttacked);
		nbt.setShort("TimeLeft", (short)this.timeLeft);
		nbt.setFloat("DamageMult", this.damageMult);
		if(this.spawnPos != null)
			nbt.setIntArray("SpawnPos", new int[]{this.spawnPos.getX(), this.spawnPos.getY(), this.spawnPos.getZ()});
	}
	@Override
	public void readEntityFromNBT(NBTTagCompound nbt) {
		super.readEntityFromNBT(nbt);
		level=nbt.getShort("Level");
		this.playersAttacked=nbt.getShort("Players");
		this.timeLeft=nbt.getShort("TimeLeft");
		this.damageMult=nbt.getFloat("DamageMult");
		if(this.timeLeft<2250)
			this.setGlowing(false);
		if(nbt.hasKey("SpawnPos")) {
			int[] arr = nbt.getIntArray("SpawnPos");
			this.spawnPos = new BlockPos(arr[0], arr[1], arr[2]);
		}
	}
	@Override
	public void addTrackingPlayer(EntityPlayerMP player) {
		super.addTrackingPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void removeTrackingPlayer(EntityPlayerMP player) {
		super.removeTrackingPlayer(player);
		this.bossInfo.removePlayer(player);
	}
	public boolean isPotionApplicable(PotionEffect potioneffectIn)
    {
		return potioneffectIn.getPotion() == TF2weapons.stun && potioneffectIn.getAmplifier()>=3;
    }
}