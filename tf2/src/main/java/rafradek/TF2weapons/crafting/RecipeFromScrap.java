package rafradek.TF2weapons.crafting;

import java.util.ArrayList;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import rafradek.TF2weapons.TF2weapons;
import rafradek.TF2weapons.characters.ItemToken;

public class RecipeFromScrap extends net.minecraftforge.registries.IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

	public boolean withToken;
	
	public RecipeFromScrap(boolean withToken) {
		this.withToken = withToken;
	}
	@Override
	public boolean matches(InventoryCrafting inv, World worldIn) {
		ArrayList<ItemStack> stacks = new ArrayList<>();

		int tokens = 0;
		for (int x = 0; x < inv.getSizeInventory(); x++) {
			ItemStack stack = inv.getStackInSlot(x);
			if (!stack.isEmpty())
				if (stack.getItem() instanceof ItemTF2 && stack.getMetadata() == 3)
					stacks.add(stack);
				else if (this.withToken && stack.getItem() instanceof ItemToken && tokens == 0)
					tokens++;
				else
					return false;
		}
		// System.out.println("matches "+(australium&&stack2!=null));
		return stacks.size() == 2;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		// TODO Auto-generated method stub
		ItemStack result = new ItemStack(TF2weapons.itemTF2, 1, 9);
		for (int x = 0; x < inv.getSizeInventory(); x++) {
			ItemStack stack = inv.getStackInSlot(x);
			if (stack.getItem() instanceof ItemToken) {
				result.setTagCompound(new NBTTagCompound());
				result.getTagCompound().setByte("Token", (byte) stack.getMetadata());
			}
		}
		return result;
	}

	@Override
	public ItemStack getRecipeOutput() {
		// TODO Auto-generated method stu
		return new ItemStack(TF2weapons.itemTF2, 1, 9);
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
    {
        NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i)
        {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
        }

        return nonnulllist;
    }

	@Override
	public boolean canFit(int width, int height) {
		// TODO Auto-generated method stub
		return width * height >= 2;
	}

}
