package rafradek.TF2weapons.building;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Predicate;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.FoodStats;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import rafradek.TF2weapons.ClientProxy;
import rafradek.TF2weapons.TF2Attribute;
import rafradek.TF2weapons.TF2ConfigVars;
import rafradek.TF2weapons.TF2Sounds;
import rafradek.TF2weapons.TF2Util;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.characters.EntityEngineer;
import rafradek.TF2weapons.characters.EntityTF2Character;
import rafradek.TF2weapons.weapons.ItemCloak;
import rafradek.TF2weapons.weapons.WeaponsCapability;

public class EntityDispenser extends EntityBuilding {

	public int reloadTimer;
	public int giveAmmoTimer;
	public List<EntityLivingBase> dispenserTarget = new ArrayList<>();
	private static final DataParameter<Integer> METAL = EntityDataManager.createKey(EntityDispenser.class,
			DataSerializers.VARINT);
	public ItemStackHandler items = new ItemStackHandler(9);
	
	public HashMap<ItemStack, Float> fillMeter = new HashMap<>();
	public int food;
	private ItemStack currRepairItem;
	
	public static final int MAX_METAL = 400;

	public EntityDispenser(World worldIn) {
		super(worldIn);
		this.setSize(1f, 1.1f);
	}

	public EntityDispenser(World worldIn, EntityLivingBase living) {
		super(worldIn, living);
		this.setSize(1f, 1.1f);
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.isDisabled()) {
			this.dispenserTarget.clear();
			return;
		}

		List<EntityLivingBase> targetList = this.world.getEntitiesWithinAABB(EntityLivingBase.class,
				this.getEntityBoundingBox().grow(2, 1.5, 2), new Predicate<EntityLivingBase>() {

					@Override
					public boolean apply(EntityLivingBase input) {

						return !(input instanceof EntityBuilding) && EntityDispenser.this != input
								&& ((getOwner() != null && WeaponsCapability.get(getOwner()).dispenserPlayer && input instanceof EntityPlayer
										&& input.getTeam() == null)
										|| TF2Util.isOnSameTeam(EntityDispenser.this, input));
					}

				});
		if (!this.world.isRemote) {
			this.reloadTimer--;
			
			if (this.reloadTimer <= 0 && this.getMetal() < MAX_METAL) {
				int metalAmount = TF2ConfigVars.fastMetalProduction ? 30 : 21;
				metalAmount = Math.min(MAX_METAL - this.getMetal(), metalAmount + this.getLevel() * (metalAmount / 3));
				if(this.consumeEnergy(metalAmount * this.getMinEnergy())) {
					this.setMetal(this.getMetal() + metalAmount);
					this.playSound(TF2Sounds.MOB_DISPENSER_GENERATE_METAL, 1.55f, 1f);
					this.reloadTimer = TF2ConfigVars.fastMetalProduction ? 100 : 200;
				}
			}
			this.giveAmmoTimer--;
			if (this.food <= 8 && this.giveAmmoTimer == 0) {
				ItemStack foodItem = TF2Util.getFirstItem(this.items, stack -> {
					return stack.getItem() instanceof ItemFood;
				});
				
				if(!foodItem.isEmpty()) {
					this.food += ((ItemFood)foodItem.getItem()).getHealAmount(foodItem);
					foodItem.shrink(1);
				}
			}
			for (EntityLivingBase living : targetList) {
				int level = this.getLevel();
				if (living.getHealth() < living.getMaxHealth() && this.consumeEnergy(this.getMinEnergy()))
					living.heal(0.025f + 0.025f * level);
				
				if (this.giveAmmoTimer == 0) {
					if (living instanceof EntityPlayer) {
						FoodStats stats = ((EntityPlayer) living).getFoodStats();
						if(stats.getFoodLevel() >= 20)
							stats.addStats(1, living.getHealth() >= living.getMaxHealth() ? 8 : 1);
						else{
							int foodAmount = (int) (living.getHealth()/living.getMaxHealth() * 8);
							if(this.food > 0) {
								stats.addStats(Math.min(foodAmount, this.food), 1f);
								this.food -= Math.min(foodAmount, this.food);
							}
						}
					}
					
					if (living instanceof EntityEngineer || living instanceof EntityPlayer) {
						int metal = living.getCapability(TF2weapons.WEAPONS_CAP, null).getMetal();
						int metalUse = Math.min(30 + this.getLevel() * 10,
								Math.min(200 - metal, this.getMetal()));
						this.setMetal(this.getMetal() - metalUse);
						living.getCapability(TF2weapons.WEAPONS_CAP, null).setMetal(metal + metalUse);
					}
					if (living instanceof EntityTF2Character) {
						((EntityTF2Character)living).restoreAmmo(0.1f+this.getLevel()*0.1f);
					
					}
					Predicate<ItemStack> test = stack -> {
						return !stack.isEmpty() && stack.isItemStackDamageable()
						&& stack.getItemDamage() != 0 && !TF2ConfigVars.repairBlacklist.contains(stack.getItem().getRegistryName());
					};
					ItemStack heldItem = TF2Util.getFirstItem(items, test);
					
					if (heldItem.isEmpty() && test.apply(living.getHeldItemMainhand()))
						heldItem = living.getHeldItemMainhand();
					if (!heldItem.isEmpty()) {

						float repairMult = TF2ConfigVars.dispenserRepair;
						NBTTagList list = heldItem.getEnchantmentTagList();
						if (list != null) {
							for (int i = 0; i < list.tagCount(); i++)
								repairMult -= list.getCompoundTagAt(i).getShort("lvl") * TF2ConfigVars.dispenserRepair / 15f;
							if (repairMult <= TF2ConfigVars.dispenserRepair / 3f)
								repairMult = TF2ConfigVars.dispenserRepair / 3f;
						}
						
						int metalUse = Math.min(15 + this.getLevel() * 10,
								Math.min(
										(int) (heldItem.getItemDamage() / repairMult) + 1,
										this.getMetal()));
						
						//System.out.println("use: "+metalUse);
						int repairUses = this.getRepairMaterialUses(heldItem, Math.min(heldItem.getItemDamage(), (int) (metalUse * repairMult)));
						//System.out.println("repair use: "+repairUses);
						if (repairUses != 0) {
							if(this.consumeEnergy(metalUse*this.getMinEnergy())) {
								this.setMetal(this.getMetal() - metalUse);
								heldItem.setItemDamage(
										heldItem.getItemDamage() - repairUses);
	
								if (living instanceof EntityPlayerMP)
									((EntityPlayerMP) living).updateHeldItem();
							}
						}
					}
				}
				Tuple<Integer, ItemStack> cloak = ItemCloak.searchForWatches(living);
				if (!cloak.getSecond().isEmpty() || (!living.getHeldItem(EnumHand.MAIN_HAND).isEmpty()
						&& living.getHeldItem(EnumHand.MAIN_HAND).getItem() instanceof ItemCloak)) {
					if (cloak.getSecond().isEmpty())
						cloak=new Tuple<>(-1,living.getHeldItemMainhand());

					if(TF2Attribute.getModifier("No External Cloak", cloak.getSecond(), 0, living) == 0) {
						cloak.getSecond().setItemDamage(Math.max(cloak.getSecond().getItemDamage() - (2 + this.getLevel()), 0));
						if (living instanceof EntityPlayerMP)
							((EntityPlayerMP) living).connection.sendPacket(
									new SPacketSetSlot(-2, cloak.getFirst(), cloak.getSecond()));
					}
				}
				if (this.dispenserTarget != null && !this.dispenserTarget.contains(living))
					this.playSound(TF2Sounds.MOB_DISPENSER_HEAL, 0.75f, 1f);
			}
			if (this.giveAmmoTimer <= 0)
				this.giveAmmoTimer = 20;
		}
		this.dispenserTarget = targetList;
	}

	@Override
	public boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (!this.world.isRemote && player == this.getOwner() && hand == EnumHand.MAIN_HAND) {
			FMLNetworkHandler.openGui(player, TF2weapons.instance, 5, world, this.getEntityId(), 0, 0);
			return true;
		}
		return true;
	}
	
	@Override
	public SoundEvent getSoundNameForState(int state) {
		switch (state) {
		case 0:
			return TF2Sounds.MOB_DISPENSER_IDLE;
		default:
			return super.getSoundNameForState(state);
		}
	}

	public int getRepairMaterialUses(ItemStack tool, int maxRepair) {
		Iterator<Entry<ItemStack,Float>> it = this.fillMeter.entrySet().iterator();
		while(it.hasNext()) {
			Entry<ItemStack,Float> uses = it.next();
			if(tool.getItem().getIsRepairable(tool, uses.getKey())) {
				this.currRepairItem = uses.getKey();
				float materialCost = this.getMaterialCost(tool);
				float use = ((float)maxRepair / (float)tool.getMaxDamage()) * materialCost;
				
				float maxUse = Math.min(uses.getValue(), use);
				uses.setValue(uses.getValue() - maxUse);
				if (uses.getValue() == 0)
					it.remove();
				return (int) (maxUse * (tool.getMaxDamage() / materialCost));
			}
		}
		
		for (int i = 0; i < this.items.getSlots(); i++) {
			if(tool.getItem().getIsRepairable(tool, this.items.getStackInSlot(i))) {
				
				this.currRepairItem = this.items.getStackInSlot(i);
				this.items.extractItem(i, 1, false);
				
				float materialCost = this.getMaterialCost(tool);
				float use = ((float)maxRepair / (float)tool.getMaxDamage()) * materialCost;
				float maxUse = Math.min(1, use);
				this.fillMeter.put(ItemHandlerHelper.copyStackWithSize(currRepairItem, 1), 1f - maxUse);
				return (int) (maxUse * (tool.getMaxDamage() / materialCost));
			}
		}
		return 0;
	}
	
	public float getMaterialCost(ItemStack stack) {
		float cost = 0;
		for(IRecipe recipe : ForgeRegistries.RECIPES.getValues()) {
			if(recipe.getRecipeOutput().isItemEqualIgnoreDurability(stack) && TF2Util.isBaseSame(recipe.getRecipeOutput().getTagCompound(), stack.getTagCompound())) {
				for (Ingredient ing : recipe.getIngredients()) {
					if(ing.apply(this.currRepairItem)) {
						cost++;
					}
						
				}
				if(cost != 0) {
					break;
				}
			}
		}
		if (cost == 0) {
			if (stack.getItem() instanceof ItemPickaxe || stack.getItem() instanceof ItemAxe)
				cost = 3;
			else if (stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemHoe || stack.getItem() instanceof ItemShears)
				cost = 2;
			else if (stack.getItem() instanceof ItemSpade)
				cost = 1;
			else if (stack.getItem() instanceof ItemArmor) {
				switch (((ItemArmor)stack.getItem()).armorType) {
				case CHEST: cost = 8; break;
				case FEET: cost = 4; break;
				case HEAD: cost = 5; break;
				case LEGS: cost = 7; break;
				default: break;
				}
			}
			else {
				cost = 4;
			}
		}
		if (stack.getItem() instanceof ItemArmor)
			cost /= 4.5f;
		else
			cost /= 3f;
		return cost;
	}
	public static boolean isNearDispenser(World world, final EntityLivingBase living) {
		List<EntityDispenser> targetList = world.getEntitiesWithinAABB(EntityDispenser.class,
				living.getEntityBoundingBox().grow(2.5D, 2D, 2.5D), new Predicate<EntityDispenser>() {

					@Override
					public boolean apply(EntityDispenser input) {

						return !input.isDisabled() && input.dispenserTarget != null
								&& input.dispenserTarget.contains(living);
					}

				});
		return !targetList.isEmpty();
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(METAL, 0);
	}

	public int getMetal() {
		return this.dataManager.get(METAL);
	}

	public void setMetal(int amount) {
		this.dataManager.set(METAL, amount);
	}
	
	@Override
	public void upgrade() {
		super.upgrade();
		this.setMetal(this.getMetal() + 25);
	}

	public int getMinEnergy() {
		return this.getOwnerId() != null ? TF2ConfigVars.dispenserUseEnergy : 0;
	}
	
	public boolean isItemStackAccepted(ItemStack stack) {
		return stack.isItemStackDamageable();
	}
	
	public void drawFromBlock(BlockPos pos, TileEntity ent, EnumFacing facing) {
		super.drawFromBlock(pos, ent, facing);
		EnumFacing front = EnumFacing.getDirectionFromEntityLiving(pos, this);
		if (ent.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite())) {
			IItemHandler handler = ent.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite());
			if (front == facing || front == facing.getOpposite())
				for(int i = 0; i < items.getSlots(); i++) {
					if (items.getStackInSlot(i).isItemStackDamageable() && items.getStackInSlot(i).getItemDamage() == 0)
					items.insertItem(i, ItemHandlerHelper.insertItem(handler, items.extractItem(i, 1, false), false), false);
				}
			else{
				for(int i = 0; i < handler.getSlots(); i++) {
					handler.insertItem(i, ItemHandlerHelper.insertItem(items, handler.extractItem(i, 1, false), false), false);
				}
			}
		}
	}
	
	public boolean shouldUseBlocks() {
		return true;
	}
	
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return TF2Sounds.MOB_DISPENSER_DEATH;
	}

	public int getIronDrop() {
		return 0 + this.getLevel();
	}
	
	@Override
	public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
		super.writeEntityToNBT(par1NBTTagCompound);

		par1NBTTagCompound.setTag("Items", this.items.serializeNBT());
		par1NBTTagCompound.setShort("Metal", (short) this.getMetal());
		
		par1NBTTagCompound.setShort("Food", (short) food);
		NBTTagList fill = new NBTTagList();
		for(Entry<ItemStack, Float> entry : this.fillMeter.entrySet()) {
			NBTTagCompound values = new NBTTagCompound();
			values.setTag("Item", entry.getKey().serializeNBT());
			values.setFloat("Fill", entry.getValue());
			fill.appendTag(values);
		}
		par1NBTTagCompound.setTag("Fill", fill);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
		super.readEntityFromNBT(par1NBTTagCompound);

		this.items.deserializeNBT(par1NBTTagCompound.getCompoundTag("Items"));
		this.setMetal(par1NBTTagCompound.getShort("Metal"));
		this.food = par1NBTTagCompound.getShort("Food");
		NBTTagList fill = par1NBTTagCompound.getTagList("Fill", 10);
		for(int i = 0; i < fill.tagCount(); i++) {
			NBTTagCompound values = (NBTTagCompound) fill.get(i);
			this.fillMeter.put(new ItemStack(values.getCompoundTag("Item")), values.getFloat("Fill"));
		}
	}
	
	protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier)
    {
		super.dropEquipment(wasRecentlyHit, lootingModifier);
		for(int i = 0; i < this.items.getSlots(); i++) {
			this.entityDropItem(this.items.getStackInSlot(i), 0);
		}
    }
	
	@SideOnly(Side.CLIENT)
	public void renderGUI(BufferBuilder renderer, Tessellator tessellator, EntityPlayer player, int width, int height, GuiIngame gui) {
        // GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.7F);
        // gui.drawTexturedModalRect(event.getResolution().getScaledWidth()/2-64,
        // event.getResolution().getScaledHeight()/2+35, 0, 0, 128, 40);
		ClientProxy.setColor(TF2Util.getTeamColor(player), 0.7f, 0, 0.25f, 0.8f);
        gui.drawTexturedModalRect(20, 2, 0, 112,124, 44);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.7F);
        gui.drawTexturedModalRect(0, 0, 0, 0, 144, 48);
        /*renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(event.getResolution().getScaledWidth() / 2 - 72, event.getResolution().getScaledHeight() / 2 + 76, 0.0D).tex(0.0D, 0.1875D).endVertex();
        renderer.pos(event.getResolution().getScaledWidth() / 2 + 72, event.getResolution().getScaledHeight() / 2 + 76, 0.0D).tex(0.5625D, 0.1875D).endVertex();
        renderer.pos(event.getResolution().getScaledWidth() / 2 + 72, event.getResolution().getScaledHeight() / 2 + 28, 0.0D).tex(0.5625D, 0D).endVertex();
        renderer.pos(event.getResolution().getScaledWidth() / 2 - 72, event.getResolution().getScaledHeight() / 2 + 28, 0.0D).tex(0.0D, 0D).endVertex();
        tessellator.draw();*/


        renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(19, 48, 0.0D).tex(0.75D, 0.75D).endVertex();
        renderer.pos(65, 48, 0.0D).tex(0.9375D, 0.75D).endVertex();
        renderer.pos(65, 0, 0.0D).tex(0.9375D, 0.5625D).endVertex();
        renderer.pos(19, 0, 0.0D).tex(0.75D, 0.5625D).endVertex();
        tessellator.draw();

        renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(67, 22, 0.0D).tex(0.9375D, 0.1875D).endVertex();
        renderer.pos(83, 22, 0.0D).tex(1D, 0.1875D).endVertex();
        renderer.pos(83, 6, 0.0D).tex(1D, 0.125D).endVertex();
        renderer.pos(67, 6, 0.0D).tex(0.9375D, 0.125D).endVertex();
        tessellator.draw();

        double imagePos = this.getLevel() == 1 ? 0.3125D : this.getLevel() == 2 ? 0.375D : 0.4375D;
        renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(50, 18, 0.0D).tex(0.9375D, 0.0625D + imagePos).endVertex();
        renderer.pos(66, 18, 0.0D).tex(1D, 0.0625D + imagePos).endVertex();
        renderer.pos(66, 2, 0.0D).tex(1D, imagePos).endVertex();
        renderer.pos(50, 2, 0.0D).tex(0.9375D, imagePos).endVertex();
        tessellator.draw();

        if (this.getLevel() < 3) {
            renderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            renderer.pos(67, 42, 0.0D).tex(0.9375D, 0.125D).endVertex();
            renderer.pos(83, 42, 0.0D).tex(1D, 0.125D).endVertex();
            renderer.pos(83, 26, 0.0D).tex(1D, 0.0625).endVertex();
            renderer.pos(67, 26, 0.0D).tex(0.9375D, 0.0625).endVertex();
            tessellator.draw();
        }
        float health = this.getHealth() / this.getMaxHealth();
        if (health > 0.33f) {
            GL11.glColor4f(0.9F, 0.9F, 0.9F, 1F);
        } else {
            GL11.glColor4f(0.85F, 0.0F, 0.0F, 1F);
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        for (int i = 0; i < health * 8; i++) {

            renderer.begin(7, DefaultVertexFormats.POSITION);
            renderer.pos(19, 39 - i * 5, 0.0D).endVertex();
            renderer.pos(9, 39 - i * 5, 0.0D).endVertex();
            renderer.pos(9, 43 - i * 5, 0.0D).endVertex();
            renderer.pos(19, 43 - i * 5, 0.0D).endVertex();
            tessellator.draw();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.33F);
        renderer.begin(7, DefaultVertexFormats.POSITION);
        renderer.pos(85, 21, 0.0D).endVertex();
        renderer.pos(140, 21, 0.0D).endVertex();
        renderer.pos(140, 7, 0.0D).endVertex();
        renderer.pos(85, 7, 0.0D).endVertex();
        tessellator.draw();

        if (this.getLevel() < 3) {
            renderer.begin(7, DefaultVertexFormats.POSITION);
            renderer.pos(85, 41, 0.0D).endVertex();
            renderer.pos(140, 41, 0.0D).endVertex();
            renderer.pos(140, 27, 0.0D).endVertex();
            renderer.pos(85, 27, 0.0D).endVertex();
            tessellator.draw();
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.85F);
        renderer.begin(7, DefaultVertexFormats.POSITION);
        renderer.pos(85, 21, 0.0D).endVertex();
        renderer.pos(85 + this.getMetal() * 0.1375D, 21, 0.0D).endVertex();
        renderer.pos(85 + this.getMetal() * 0.1375D, 7, 0.0D).endVertex();
        renderer.pos(85, 7, 0.0D).endVertex();
        tessellator.draw();

        if (this.getLevel() < 3) {
            renderer.begin(7, DefaultVertexFormats.POSITION);
            renderer.pos(85, 41, 0.0D).endVertex();
            renderer.pos(85 + this.getProgress() * 0.275D, 41, 0.0D)
                    .endVertex();
            renderer.pos(85 + this.getProgress() * 0.275D, 27, 0.0D)
                    .endVertex();
            renderer.pos(85, 27, 0.0D).endVertex();
            tessellator.draw();
        }
	}
}
