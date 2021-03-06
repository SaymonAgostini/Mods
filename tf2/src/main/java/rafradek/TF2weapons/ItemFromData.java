package rafradek.TF2weapons;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import rafradek.TF2weapons.TF2Attribute.State;
import rafradek.TF2weapons.WeaponData.PropertyType;
import rafradek.TF2weapons.building.EntityDispenser;
import rafradek.TF2weapons.characters.EntityTF2Character;
import rafradek.TF2weapons.weapons.ItemAmmo;
import rafradek.TF2weapons.weapons.ItemUsable;
import rafradek.TF2weapons.weapons.ItemWeapon;

public class ItemFromData extends Item implements IItemOverlay{

	public static final WeaponData BLANK_DATA = new WeaponData("toloadfiles");
	public static final Predicate<WeaponData> VISIBLE_WEAPON = new Predicate<WeaponData>() {

		@Override
		public boolean apply(WeaponData input) {
			// TODO Auto-generated method stub
			return !input.getBoolean(PropertyType.HIDDEN) && input.getInt(PropertyType.ROLL_HIDDEN) == 0
					&& !input.getString(PropertyType.CLASS).equals("cosmetic")
					&& !input.getString(PropertyType.CLASS).equals("crate");
		}

	};
	public ItemFromData() {
		this.setCreativeTab(TF2weapons.tabutilitytf2);
		this.setUnlocalizedName("tf2usable");
		this.setMaxStackSize(1);
		this.setNoRepair();
		// TODO Auto-generated constructor stub
	}

	@Override
	@SideOnly(Side.CLIENT)
	public CreativeTabs getCreativeTab() {
		return TF2weapons.tabutilitytf2;
	}

	@Override
	public void onUpdate(ItemStack stack, World par2World, Entity par3Entity, int par4, boolean par5) {
		if (getData(stack) == BLANK_DATA && par3Entity instanceof EntityPlayer) {
			((EntityPlayer) par3Entity).inventory.setInventorySlotContents(par4, ItemStack.EMPTY);
			stack.setCount( 0);
			return;
		}
	}

	public static WeaponData getData(ItemStack stack) {
		WeaponData value = BLANK_DATA;
		if(!stack.isEmpty() && stack.hasCapability(TF2weapons.WEAPONS_DATA_CAP, null)) {
			value=stack.getCapability(TF2weapons.WEAPONS_DATA_CAP, null).inst;
			if (value == BLANK_DATA && stack.hasTagCompound() && MapList.nameToData.containsKey(stack.getTagCompound().getString("Type")))
				value = stack.getCapability(TF2weapons.WEAPONS_DATA_CAP, null).inst = MapList.nameToData.get(stack.getTagCompound().getString("Type"));
		}
		return value;	
	}
	@Override
	public void getSubItems(CreativeTabs par2CreativeTabs, NonNullList<ItemStack> par3List) {
		if(!this.isInCreativeTab(par2CreativeTabs))
			return;
		Iterator<Entry<String, WeaponData>> iterator = MapList.nameToData.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, WeaponData> entry = iterator.next();
			// System.out.println("Hidden:
			// "+entry.getValue().hasProperty(PropertyType.HIDDEN));
			if (entry.getValue().hasProperty(PropertyType.HIDDEN) && entry.getValue().getBoolean(PropertyType.HIDDEN))
				continue;
			Item item = MapList.weaponClasses.get(entry.getValue().getString(PropertyType.CLASS));
			if (item == this)
				par3List.add(ItemFromData.getNewStack(entry.getKey()));
		}
	}

	public static ItemStack getNewStack(String type) {
		// "+MapList.weaponClasses.get(MapList.nameToCC.get(type).get("Class").getString())+"
		// "+Thread.currentThread().getName());
		if(!MapList.nameToData.containsKey(type))
			return ItemStack.EMPTY;
		
		return getNewStack(MapList.nameToData.get(type));
	}
	
	public static ItemStack getNewStack(WeaponData type) {
		ItemStack stack = new ItemStack(
				MapList.weaponClasses.get(type.getString(PropertyType.CLASS)));
		//System.out.println(stack.hasCapability(TF2weapons.WEAPONS_DATA_CAP, null));
		stack.getCapability(TF2weapons.WEAPONS_DATA_CAP, null).inst=type;
		NBTTagCompound tag=new NBTTagCompound();
		tag.setString("Type", type.getName());
		tag.setTag("Attributes", new NBTTagCompound());
		stack.setTagCompound(tag);
		// System.out.println(stack.toString());
		return stack;
	}
	
	public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt)
    {
        return new WeaponData.WeaponDataCapability();
    }
	
	public static List<ItemStack> getRandomWeapons(Random random, Predicate<WeaponData> predicate, int count) {

		ArrayList<WeaponData> weapons = new ArrayList<>();
		for (Entry<String, WeaponData> entry : MapList.nameToData.entrySet())
			if (predicate.apply(entry.getValue())){
				weapons.add(entry.getValue());
			}
		ArrayList<ItemStack> ret = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			if(weapons.isEmpty())
				break;
			ret.add(getNewStack(weapons.remove(random.nextInt(weapons.size()))));
		}
		return ret;
	}
	public static ItemStack getRandomWeapon(Random random, Predicate<WeaponData> predicate) {
		return Iterables.getFirst(getRandomWeapons(random, predicate, 1), ItemStack.EMPTY);
	}
	public static ItemStack getRandomWeaponOfType(String type, float chanceOfParent, Random random) {
		// WeaponData parent=MapList.nameToData.get(type);
		if (chanceOfParent >= 0 && random.nextFloat() <= chanceOfParent)
			return getNewStack(type);
		else {
			ArrayList<String> weapons = new ArrayList<>();
			if (chanceOfParent < 0)
				weapons.add(type);
			for (Entry<String, WeaponData> entry : MapList.nameToData.entrySet())
				if (!entry.getValue().getBoolean(PropertyType.HIDDEN)
						&& entry.getValue().getInt(PropertyType.ROLL_HIDDEN) == 0
						&& entry.getValue().getString(PropertyType.BASED_ON).equals(type))
					weapons.add(entry.getKey());
			if (weapons.size() > 0)
				return getNewStack(weapons.get(random.nextInt(weapons.size())));
			else
				return getNewStack(type);
		}

	}

	public static ItemStack getRandomWeaponOfClass(String clazz, Random random, boolean showHidden) {
		ArrayList<WeaponData> weapons = new ArrayList<>();
		for (Entry<String, WeaponData> entry : MapList.nameToData.entrySet())
			if (!entry.getValue().getBoolean(PropertyType.HIDDEN)
					&& (showHidden || entry.getValue().getInt(PropertyType.ROLL_HIDDEN) == 0)
					&& entry.getValue().getString(PropertyType.CLASS).equals(clazz))
				weapons.add(entry.getValue());
		return getNewStack(weapons.get(random.nextInt(weapons.size())));
	}

	public static ItemStack getRandomWeaponOfSlotMob(final String mob, final int slot, Random random,
			final boolean showHidden, boolean weighted, boolean stockOnly) {
		Predicate<WeaponData> base=new Predicate<WeaponData>() {

			@Override
			public boolean apply(WeaponData input) {
				// TODO Auto-generated method stub
				return !input.getBoolean(PropertyType.HIDDEN) && !(input.getInt(PropertyType.ROLL_HIDDEN)>0 && !showHidden)
						&& input.getInt(PropertyType.SLOT) == slot
						&& input.getString(PropertyType.MOB_TYPE).contains(mob);
			}

		};
		
		if(!weighted && !stockOnly)
			return getRandomWeapon(random, base);
		
		ItemStack stock=getRandomWeapon(random, Predicates.and(base,new Predicate<WeaponData>(){

			@Override
			public boolean apply(WeaponData input) {
				
				return input.getBoolean(PropertyType.STOCK);
			}
			
		}));
		
		if (stockOnly)
			return stock;
		Predicate<WeaponData> unipredicate=Predicates.and(base,new Predicate<WeaponData>(){

			@Override
			public boolean apply(WeaponData input) {
				
				return !input.getBoolean(PropertyType.STOCK);
			}
			
		
		});
		float unicount=getWeaponCount(unipredicate);
		ItemStack uni=getRandomWeapon(random, unipredicate);
		if(uni.isEmpty()){
			return stock;
		}
		else if(stock.isEmpty()){
			return uni;
		}
		else if(random.nextFloat()<unicount/(unicount+2f)){
			return uni;
		}
		else{
			return stock;
		}
	}
	public static List<ItemStack> getRandomWeaponsOfSlotMob(final String mob, final int slot, Random random,
			final boolean showHidden, int count) {
		return getRandomWeapons(random,new Predicate<WeaponData>() {

			@Override
			public boolean apply(WeaponData input) {
				// TODO Auto-generated method stub
				return !input.getBoolean(PropertyType.HIDDEN) && !(input.getInt(PropertyType.ROLL_HIDDEN)>0 && !showHidden)
						&& input.getInt(PropertyType.SLOT) == slot
						&& input.getString(PropertyType.MOB_TYPE).contains(mob);
			}

		}, count);
	}
	public static int getWeaponCount(Predicate<WeaponData> predicate){
		int count=0;
		for(Entry<String,WeaponData> entry:MapList.nameToData.entrySet()){
			if(predicate.apply(entry.getValue()))
				count++;
		}
		return count;
	}
	public static boolean isSameType(ItemStack stack, String name){
		return !stack.isEmpty() && getData(stack)!=BLANK_DATA && (getData(stack).getName().equals(name) || getData(stack).getString(PropertyType.BASED_ON).equals(name));
	}
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return getData(oldStack) != getData(newStack) || (slotChanged);
		
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		if (ItemFromData.getData(stack) == BLANK_DATA)
			return "Weapon";
		String name = ItemFromData.getData(stack).getString(PropertyType.NAME);
		if (stack.getTagCompound().getBoolean("Strange"))
			name = TextFormatting.GOLD
					+ TF2EventsCommon.STRANGE_TITLES[stack.getTagCompound().getInteger("StrangeLevel")] + " "
					+ name;
		if (stack.getTagCompound().getBoolean("Australium"))
			name = TextFormatting.GOLD + "Australium " + name;
		if (stack.getTagCompound().getBoolean("Valve"))
			name = TextFormatting.DARK_PURPLE + "Valve " + name;
		return name;
	}

	public static SoundEvent getSound(ItemStack stack, PropertyType name) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(getData(stack).getString(name)));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> tooltip,
			ITooltipFlag advanced) {
		/*
		 * if (!par1ItemStack.hasTagCompound()) {
		 * par1ItemStack.getTagCompound()=new NBTTagCompound();
		 * par1ItemStack.getTagCompound().setTag("Attributes", (NBTTagCompound)
		 * ((ItemUsable)par1ItemStack.getItem()).buildInAttributes.copy()); }
		 */
		if (stack.hasTagCompound()) {
			NBTTagCompound attributeList = stack.getTagCompound().getCompoundTag("Attributes");
			//attributeList.merge(MapList.buildInAttributes.get(getData(par1ItemStack).getName()));
			Iterator<String> iterator = attributeList.getKeySet().iterator();
			while (iterator.hasNext()) {
				String name = iterator.next();
				NBTBase tag = attributeList.getTag(name);
				if (tag instanceof NBTTagFloat) {
					NBTTagFloat tagFloat = (NBTTagFloat) tag;
					TF2Attribute attribute = TF2Attribute.attributes[Integer.parseInt(name)];
					//System.out.println("Attribute id: "+name);
					if (attribute != null && attribute.state != State.HIDDEN )
						tooltip.add(attribute.getTranslatedString(tagFloat.getFloat(), true));
				}
			}
			attributeList = MapList.buildInAttributes.get(getData(stack).getName());
			//attributeList.merge(MapList.buildInAttributes.get(getData(par1ItemStack).getName()));
			iterator = attributeList.getKeySet().iterator();
			while (iterator.hasNext()) {
				String name = iterator.next();
				NBTBase tag = attributeList.getTag(name);
				if (tag instanceof NBTTagFloat) {
					NBTTagFloat tagFloat = (NBTTagFloat) tag;
					TF2Attribute attribute = TF2Attribute.attributes[Integer.parseInt(name)];
					//System.out.println("Attribute id: "+name);
					if (attribute != null && attribute.state != State.HIDDEN )
						tooltip.add(attribute.getTranslatedString(tagFloat.getFloat(), true));
				}
			}
			if (getData(stack).hasProperty(PropertyType.DESC)) {
				tooltip.add("");
				for(String line:getData(stack).getString(PropertyType.DESC).split("\n"))
					tooltip.add(line);
			}
			if (stack.getTagCompound().getBoolean("Bought")) {
				tooltip.add("");
				tooltip.add("This item cannot be destroyed");
			}
		}
	}
	public int getEntityLifespan(ItemStack itemStack, World world)
    {
        return 12000;
    }
	public boolean showInfoBox(ItemStack stack, EntityPlayer player){
		return false;
	}
	public String[] getInfoBoxLines(ItemStack stack, EntityPlayer player){
		return null;
	}

	@Override
	public void drawOverlay(ItemStack stack, EntityPlayer player, Tessellator tesselator, BufferBuilder buffer, ScaledResolution resolution) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean isAmmoSufficient(ItemStack stack, EntityLivingBase living, boolean all) {
		return true;
	}
	
	public void consumeAmmoGlobal(EntityLivingBase living, ItemStack stack, int amount) {
		if (EntityDispenser.isNearDispenser(living.world, living))
			return;
		amount = this.getActualAmmoUse(stack, living, amount);
		if (living instanceof EntityTF2Character)
			((EntityTF2Character)living).useAmmo(amount);
		if (!(living instanceof EntityPlayer) || ((EntityPlayer)living).isCreative())
			return;
		if (TF2Attribute.getModifier("Metal Ammo", stack, 0, living) != 0) {
			living.getCapability(TF2weapons.WEAPONS_CAP, null).consumeMetal(amount, false);
		}
		if (amount > 0) {
			// int
			// type=ItemFromData.getData(stack).getInt(PropertyType.AMMO_TYPE);

			// stack.getCount()-=amount;
			ItemStack stackAmmo;
			while (amount > 0 && !(stackAmmo = searchForAmmo(living, stack)).isEmpty()) {
				if (stackAmmo.getItem() instanceof ItemAmmo) {
					amount = ((ItemAmmo) stackAmmo.getItem()).consumeAmmo(living, stackAmmo, amount);
				}
				else {
					if (stackAmmo.getItem() instanceof ItemArrow)
						stack.getTagCompound().setTag("LastLoaded", stackAmmo.serializeNBT());
					amount -= stackAmmo.getCount();
					stackAmmo.shrink(amount);
					
				}
				
			}
		}
	}

	public ItemStack searchForAmmo(EntityLivingBase owner, ItemStack stack) {
		if (EntityDispenser.isNearDispenser(owner.world, owner) || (owner instanceof EntityPlayer && ((EntityPlayer)owner).capabilities.isCreativeMode))
			return ItemAmmo.STACK_FILL;

		int type = ((ItemUsable) stack.getItem()).getAmmoType(stack);

		if (type == 0 || (type == 14 && TF2ConfigVars.freeUseItems))
			return ItemAmmo.STACK_FILL;
		
		if (owner instanceof EntityTF2Character) {
			return ((EntityTF2Character)owner).getAmmo(ItemFromData.getData(stack).getInt(PropertyType.SLOT)) > 0 ? ItemAmmo.STACK_FILL : ItemStack.EMPTY;
		}
		
		int metalammo = (int) TF2Attribute.getModifier("Metal Ammo", stack, 0, owner);
		if (metalammo != 0) {
			return owner.getCapability(TF2weapons.WEAPONS_CAP, null).hasMetal(metalammo) ? ItemAmmo.STACK_FILL : ItemStack.EMPTY;
		}
		
		if (owner.world.isRemote && (type >= owner.getCapability(TF2weapons.PLAYER_CAP, null).cachedAmmoCount.length || owner.getCapability(TF2weapons.PLAYER_CAP, null).cachedAmmoCount[type] > 0))
			return ItemAmmo.STACK_FILL;
		
		if (!owner.getCapability(TF2weapons.INVENTORY_CAP, null).getStackInSlot(3).isEmpty()){
			IItemHandler inv=owner.getCapability(TF2weapons.INVENTORY_CAP, null).getStackInSlot(3)
					.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			//System.out.println("Ammo Search: "+inv.getSlots());
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack stackCap = inv.getStackInSlot(i);
				//System.out.println("Stack: "+stackCap);
				if (!stackCap.isEmpty() && ((stackCap.getItem() instanceof ItemAmmo
						&& ((ItemAmmo) stackCap.getItem()).getTypeInt(stackCap) == type) || (type == 1000 && stackCap.getItem() instanceof ItemArrow)) ){
					//System.out.println("Found: "+i);
					return stackCap;
				}
			}
		}
		for (int i = 0; i < ((EntityPlayer) owner).inventory.mainInventory.size(); i++) {
			ItemStack stackInv = ((EntityPlayer) owner).inventory.mainInventory.get(i);
			if (stackInv != null && ((stackInv.getItem() instanceof ItemAmmo
					&& ((ItemAmmo) stackInv.getItem()).getTypeInt(stackInv) == type) || (type == 1000 && stackInv.getItem() instanceof ItemArrow)))
				return stackInv;
		}
		return ItemStack.EMPTY;
	}

	public int getAmmoType(ItemStack stack) {
		return TF2Attribute.getModifier("No Ammo", stack, 0, null) != 0 ? 0:getData(stack).getInt(PropertyType.AMMO_TYPE);
	}
	
	public int getAmmoAmount(EntityLivingBase owner, ItemStack stack) {
		
		int type = this.getAmmoType(stack);
		
		if (type == 0)
			return 999;
		
		if (type == 14 && owner instanceof EntityPlayer && TF2ConfigVars.freeUseItems) {
			return ((EntityPlayer)owner).getCooldownTracker().hasCooldown(this) ? 0 : 1;
		}
		if (EntityDispenser.isNearDispenser(owner.world, owner) || (owner instanceof EntityPlayer && ((EntityPlayer)owner).capabilities.isCreativeMode))
			return 999;

		if (owner instanceof EntityTF2Character)
			return (int) (((EntityTF2Character) owner).getAmmo() / TF2Attribute.getModifier("Ammo Eff", stack, 1, owner));

		if(TF2Attribute.getModifier("Ball Release", stack, 0, owner)>0)
			stack=ItemFromData.getNewStack("sandmanball");
		

		if (TF2Attribute.getModifier("Metal Ammo", stack, 0, owner) != 0) {
			return owner.getCapability(TF2weapons.WEAPONS_CAP, null).getMetal();
		}
		
		

		int ammoCount = 0;

		if (!owner.getCapability(TF2weapons.INVENTORY_CAP, null).getStackInSlot(3).isEmpty()){
			IItemHandler inv=owner.getCapability(TF2weapons.INVENTORY_CAP, null).getStackInSlot(3)
			.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			for (int i = 0; i < inv.getSlots(); i++) {
				ItemStack stackCap = inv.getStackInSlot(i);
				if (!stackCap.isEmpty() && stackCap.getItem() instanceof ItemAmmo
						&& ((ItemAmmo) stackCap.getItem()).getTypeInt(stackCap) == type){
					//System.out.println("Found: "+i);
					ammoCount += ((ItemAmmo) stackCap.getItem()).getAmount(stackCap);
				}
				else if (type == 1000 && !stackCap.isEmpty() && stackCap.getItem() instanceof ItemArrow)
					ammoCount += stackCap.getCount();
			}
		}
		for (int i = 0; i < ((EntityPlayer) owner).inventory.mainInventory.size(); i++) {
			ItemStack stackInv = ((EntityPlayer) owner).inventory.mainInventory.get(i);
			if (!stackInv.isEmpty() && stackInv.getItem() instanceof ItemAmmo
					&& ((ItemAmmo) stackInv.getItem()).getTypeInt(stackInv) == type)
				ammoCount += ((ItemAmmo) stackInv.getItem()).getAmount(stackInv);
			else if (type == 1000 && !stackInv.isEmpty() && stackInv.getItem() instanceof ItemArrow)
				ammoCount += stackInv.getCount();
		}
		return (int) (ammoCount / TF2Attribute.getModifier("Ammo Eff", stack, 1, owner));
	}
	
	public int getActualAmmoUse(ItemStack stack, EntityLivingBase living, int amount) {
		if (this.getAmmoType(stack) == 0 || amount == 0)
			return 0;

		if (TF2Attribute.getModifier("Metal Ammo", stack, 0, living) != 0) {
			amount = (int) TF2Attribute.getModifier("Metal Ammo", stack, 0, living);
		}
		stack.getTagCompound().setFloat("UsedAmmo", stack.getTagCompound().getFloat("UsedAmmo")
				+ amount * TF2Attribute.getModifier("Ammo Eff", stack, 1, living));
		amount = 0;
		while (stack.getTagCompound().getFloat("UsedAmmo") >= 1) {
			stack.getTagCompound().setFloat("UsedAmmo", stack.getTagCompound().getFloat("UsedAmmo") - 1);
			amount++;
		}
		return amount;
	}
}
