package rafradek.TF2weapons.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import rafradek.TF2weapons.ItemFromData;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.weapons.ItemCloak;
import rafradek.TF2weapons.weapons.ItemUsable;

public class AustraliumRecipe implements IRecipe {

	public static final ItemStack ITEM = ItemFromData.getNewStack("minigun");

	static {
		ITEM.getTagCompound().setBoolean("Australium", true);
		ITEM.getTagCompound().setBoolean("Strange", true);
	}

	@Override
	public boolean matches(InventoryCrafting inv, World worldIn) {
		int australium = 0;
		ItemStack stack2 = ItemStack.EMPTY;

		for (int x = 0; x < inv.getSizeInventory(); x++) {
			ItemStack stack = inv.getStackInSlot(x);
			if (!stack.isEmpty())
				if (stack.getItem() == TF2weapons.itemTF2 && stack.getMetadata() == 2) {
					if (australium < 8)
						australium++;
					else
						return false;
				} else if (stack2.isEmpty() && (stack.getItem() instanceof ItemTool
						|| stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemBow
						|| stack.getItem() instanceof ItemUsable || stack.getItem() instanceof ItemCloak))
					stack2 = stack;
				else
					return false;
		}
		// System.out.println("matches "+(australium&&stack2!=null));
		return australium == 8 && !stack2.isEmpty();
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		// TODO Auto-generated method stub
		ItemStack stack2 = ItemStack.EMPTY;

		for (int x = 0; x < inv.getSizeInventory(); x++) {
			ItemStack stack = inv.getStackInSlot(x);
			if (!stack.isEmpty())
				if (!(stack.getItem() == TF2weapons.itemTF2 && stack.getMetadata() == 2))
					stack2 = stack;
		}
		// System.out.println("OutPut: "+stack2);
		if (!stack2.isEmpty()) {
			stack2 = stack2.copy();
			if (!stack2.hasTagCompound())
				stack2.setTagCompound(new NBTTagCompound());
			stack2.setCount( 1);
			stack2.getTagCompound().setBoolean("Australium", true);
			//stack2.getTagCompound().setBoolean("Strange", true);
		}
		return stack2;
	}

	@Override
	public ItemStack getRecipeOutput() {
		// TODO Auto-generated method stu
		return ITEM;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
		// TODO Auto-generated method stub
		NonNullList<ItemStack> aitemstack = NonNullList.withSize(inv.getSizeInventory(),ItemStack.EMPTY);

		for (int i = 0; i < aitemstack.size(); ++i) {
			ItemStack itemstack = inv.getStackInSlot(i);
			aitemstack.set(i,net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
		}

		return aitemstack;
	}

	@Override
	public IRecipe setRegistryName(ResourceLocation name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceLocation getRegistryName() {
		// TODO Auto-generated method stub
		return new ResourceLocation(TF2weapons.MOD_ID, "australium_recipe");
	}

	@Override
	public Class<IRecipe> getRegistryType() {
		// TODO Auto-generated method stub
		return IRecipe.class;
	}

	@Override
	public boolean canFit(int width, int height) {
		// TODO Auto-generated method stub
		return width >= 3 && height >=3;
	}

}
