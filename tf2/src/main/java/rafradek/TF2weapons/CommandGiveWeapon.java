package rafradek.TF2weapons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CommandGiveWeapon extends CommandBase {

	@Override
	public String getUsage(ICommandSender p_71518_1_) {
		// TODO Auto-generated method stub
		return "commands.giveweapon.usage";
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "giveweapon";
	}
	
	public int getRequiredPermissionLevel()
    {
        return 2;
    }

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1)
			throw new WrongUsageException(getUsage(sender), new Object[0]);
		try {
			ItemStack item = ItemFromData.getNewStack(args[0]);
			NBTTagCompound attributes = item.getTagCompound().getCompoundTag("Attributes");
			EntityPlayerMP entityplayermp = args.length > 1 ? getPlayer(server, sender, args[1])
					: getCommandSenderAsPlayer(sender);
			for (int i = 2; i < args.length; i++) {
				String[] attr = args[i].split(":");
				if(attr.length==2){
					if(attr[0].equals("u"))
						item.getTagCompound().setByte("UEffect", Byte.parseByte(attr[1]));
					else if(MapList.nameToAttribute.containsKey(attr[0]))
						attributes.setFloat(Integer.toString(MapList.nameToAttribute.get(attr[0]).id), Float.parseFloat(attr[1]));
					else if(TF2Attribute.attributes[Integer.parseInt(attr[0])]!=null)
						attributes.setFloat(attr[0], Float.parseFloat(attr[1]));
					
				}
				else if(attr[0].equals("a"))
					item.getTagCompound().setBoolean("Australium", true);
				else if(attr[0].equals("s"))
					item.getTagCompound().setBoolean("Strange", true);
				else if(attr[0].equals("v"))
					item.getTagCompound().setBoolean("Valve", true);
					
			}
			
			EntityItem entityitem = entityplayermp.dropItem(item, false);
			entityitem.setPickupDelay(0);
			/*Chunk chunk=entityplayermp.world.getChunkFromBlockCoords(entityplayermp.getPosition());
			int ausCount=0;
			int leadCount=0;
			int coppCount=0;
			int diaCount=0;
			for(int x=0;x<16;x++){
				for(int y=0; y<256;y++){
					for(int z=0; z<16;z++){
						IBlockState state=chunk.getBlockState(x, y, z);
						if(state.getBlock()==TF2weapons.blockAustraliumOre)
							ausCount++;
						else if(state.getBlock()==TF2weapons.blockLeadOre)
							leadCount++;
						else if(state.getBlock()==TF2weapons.blockCopperOre)
							coppCount++;
						else if(state.getBlock()==Blocks.DIAMOND_ORE)
							diaCount++;
					}
				}
			}*/
			
			// notifyCommandListener(sender, this, "Found ores:"+ausCount+" "+leadCount+" "+coppCount+" "+diaCount, new Object[0]);
			// entityitem.func_145797_a(entityplayermp.getCommandSenderName());
			// func_152373_a(p_71515_1_, this, "commands.giveweapon.success",
			// new Object[] {item.func_151000_E(),
			// entityplayermp.getCommandSenderName()});
		} catch (Exception e) {
			throw new WrongUsageException(getUsage(sender), new Object[0]);
		}
	}
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
		if (args.length == 1)
        {
            return getListOfStringsMatchingLastWord(args, MapList.nameToData.keySet());
        }
		else if (args.length == 2)
        {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        else
        {
            return !args[args.length - 1].contains(":") ? getListOfStringsMatchingLastWord(args, MapList.nameToAttribute.keySet()) : Collections.emptyList();
        }
    }
}
