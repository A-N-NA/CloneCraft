package net.jamezo97.clonecraft.clone;

import java.util.ArrayList;
import java.util.Hashtable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.jamezo97.clonecraft.build.CustomBuilders;
import net.jamezo97.clonecraft.build.PlantCustomBuilder;
import net.jamezo97.clonecraft.network.Handler9UpdateBreakBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOre;
import net.minecraft.block.IGrowable;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class BreakableBlocks
{

	CloneOptions options;

	EntityClone clone;

	public boolean isDirty = false;

	ArrayList<Long> selectedBlocks = new ArrayList<Long>();

	public BreakableBlocks(CloneOptions options)
	{
		this.options = options;
		this.clone = options.clone;
		if(validBlocksArray == null || validBlocksArray.length == 0)
		{
			loadBlocksClient();
		}
	}

	public boolean isDirty()
	{
		return isDirty;
	}

	public BreakableBlocks setDirty(boolean dirty)
	{
		this.isDirty = dirty;
		return this;
	}

	public NBTTagCompound save(NBTTagCompound nbt)
	{
		int[] data = toPrimitiveInt(this.selectedBlocks);
		nbt.setIntArray("BrkBlc", data);
		return nbt;
	}

	public NBTTagCompound load(NBTTagCompound nbt)
	{
		int[] data = nbt.getIntArray("BrkBlc");
		addPrimitiveInt(this.selectedBlocks, data);
		setDirty(false);
		return nbt;
	}

	public void toggleBlock(long block)
	{
		set(block, !canBreak(block));
	}

	public int set(long blockData, boolean selected)
	{
		if (!selected && selectedBlocks.contains(blockData))
		{
			selectedBlocks.remove((Object) blockData);
			setDirty(true);
			return -1;
		}

		if (!isValidBlock(blockData))
		{
			return 0;
		}

		if (selected && !selectedBlocks.contains(blockData))
		{
			selectedBlocks.add(blockData);
			setDirty(true);
			return 1;
		}

		return 0;
	}

	public boolean canBreak(long block)
	{
		return selectedBlocks.contains(block);
	}

	public boolean canBreak(int x, int y, int z)
	{		
		try
		{
			Block block = clone.worldObj.getBlock(x, y, z);
			if (block == Blocks.air || (block.getMaterial() != null && block.getMaterial().isLiquid()))
			{
				return false;
			}
			Item blockItem = block.getItem(clone.worldObj,x, y, z);
			int id = Block.getIdFromBlock(block);			
			int meta = clone.worldObj.getBlockMetadata(x, y, z);
			
			if (clone.getOptions().farming.get())
			{
				// If it's a fully grown growable block, and it's not a bush
				if (block instanceof IGrowable && CustomBuilders.customBuilderMap.get(block) instanceof PlantCustomBuilder)
				{
					if (!((IGrowable) block).func_149851_a(clone.worldObj, x, y, z, true))
					{
						return true;
					}
				}
			}
			long conjoinedData = this.conjoin(id, meta);
			if(canBreak(conjoinedData))
			{
				return true;
			}
			//return canBreak(this.conjoin(id, meta));
			//Handle gregtech metatile block
			else if(BreakableBlocks.BlockSubItems.containsKey(id))
			{
				ArrayList subBlocks = BreakableBlocks.BlockSubItems.get(id);
				boolean canBreak = false;
//				ArrayList loadInto = new ArrayList();
//				blockItem.getSubItems(blockItem, CreativeTabs.tabAllSearch, loadInto);
				
				ItemStack stack;
	
				for (int a = 0; a < subBlocks.size(); a++)
				{
					//if (subBlocks.get(a) instanceof ItemStack)
					//{															
						stack = (ItemStack) subBlocks.get(a);
						
						int itemDamage =  stack.getItemDamage();								
						long data = conjoin(id,itemDamage);
						canBreak = canBreak(data);
						if(canBreak)
						{
							return true;
						}	
					//}
				}
				return false;
			
			}
			else
			{
				return false;
			}
			
			//return canBreak(this.conjoin(id, maxDamage)); //meta));
		}
		catch(Throwable t){return false;}//Why. At least it won't crash.
		
	}

	public void tick(EntityClone clone)
	{
		if (clone.worldObj.isRemote)
		{
			if (isDirty())
			{
				this.setDirty(false);
				Handler9UpdateBreakBlocks handler = new Handler9UpdateBreakBlocks(clone.getEntityId(), this.toPrimitiveLong(selectedBlocks));
				handler.sendToServer();
			}
		}
		else
		{
			if (isDirty())
			{
				this.setDirty(false);
				Handler9UpdateBreakBlocks handler = new Handler9UpdateBreakBlocks(clone.getEntityId(), this.toPrimitiveLong(selectedBlocks));
				handler.sendToOwnersWatching(clone);
			}
		}

	}

	public void clear()
	{
		if (selectedBlocks.size() != 0)
		{
			setDirty(true);
			this.selectedBlocks.clear();
		}

	}

	public void importLong(long[] data)
	{
		int prev = this.selectedBlocks.size();
		addPrimitiveLong(this.selectedBlocks, data);

		if (this.selectedBlocks.size() != prev)
		{
			this.setDirty(true);
		}
	}

	public void importInt(int[] data)
	{
		int prev = this.selectedBlocks.size();
		addPrimitiveInt(this.selectedBlocks, data);

		if (this.selectedBlocks.size() != prev)
		{
			this.setDirty(true);
		}
	}

	public long[] exportLong()
	{
		return this.toPrimitiveLong(selectedBlocks);
	}

	public int[] exportInt()
	{
		return this.toPrimitiveInt(selectedBlocks);
	}

	public ArrayList<Long> getArray()
	{
		return selectedBlocks;
	}

	public void selectAll()
	{
		this.clear();
		this.addPrimitiveLong(selectedBlocks, validBlocksArray);
		this.setDirty(true);
	}

	public void selectOres()
	{
		for (int a = 0; a < validBlocksArray.length; a++)
		{
			Block block = Block.getBlockById(this.getId(validBlocksArray[a]));

			if (block != null)
			{
				if (block instanceof BlockOre)
				{
					this.set(validBlocksArray[a], true);
				}
			}
		}
	}

	public void selectTrees()
	{
		for (int a = 0; a < validBlocksArray.length; a++)
		{
			Block block = Block.getBlockById(this.getId(validBlocksArray[a]));

			if (block != null)
			{
				if (block instanceof BlockLog)
				{
					this.set(validBlocksArray[a], true);
				}
			}
		}
	}

	public static long conjoin(int id, int meta)
	{
		return (((long) id) << 32) | meta;
	}

	public static int getId(long data)
	{
		return (int) ((data >> 32) & 0xffffffff);
	}

	public static int getMeta(long data)
	{
		return (int) (data & 0xffffffff);
	}

	private static ArrayList<Long> validBlocks = new ArrayList<Long>();

	public static long[] validBlocksArray;
	public static Hashtable<Integer,ArrayList<ItemStack>> BlockSubItems;

	@SideOnly(value = Side.CLIENT)
	public void loadBlocksClient()
	{

		BlockSubItems = new Hashtable<Integer,ArrayList<ItemStack>>();
		for (Object o : Block.blockRegistry)
		{
			if (o instanceof Block)
			{
				//loadInto.clear();
				Block block = (Block) o;
				int blockId = Block.getIdFromBlock(block);
				Item blockItem = Item.getItemFromBlock(block);

				if (blockItem != null && block.getCreativeTabToDisplayOn() != null)
				{
					try
					{
						ArrayList loadInto = new ArrayList();
						blockItem.getSubItems(blockItem, CreativeTabs.tabAllSearch, loadInto);
						BlockSubItems.put(blockId, loadInto);
						ItemStack stack;

						for (int a = 0; a < loadInto.size(); a++)
						{
							if (loadInto.get(a) instanceof ItemStack)
							{															
								stack = (ItemStack) loadInto.get(a);
								
								int itemDamage =  stack.getItemDamage();								
								long data = conjoin(blockId,itemDamage);
//								if(stack.toString().contains("gt."))
//								{
//									System.err.println(stack.toString() + ":" + Block.getIdFromBlock(block) + ":" + itemDamage);
//								}
								//long data = conjoin(Block.getIdFromBlock(block), block.hashCode());
								if (!validBlocks.contains(data))
								{
									validBlocks.add(data);
								}
							}
						}
					}
					catch(Throwable e)
					{
						System.err.println("Failed to load sub items from block " + blockItem);
						e.printStackTrace();
					}
					
				}
			}
		}

		validBlocksArray = toPrimitiveLong(validBlocks);
	}

	public static boolean isValidBlock(long blockData)
	{
		return validBlocks.contains(blockData);
	}

	public static long[] toPrimitiveLong(ArrayList<Long> list)
	{
		long[] data = new long[list.size()];
		for (int a = 0; a < list.size(); a++)
		{
			data[a] = list.get(a);
		}
		return data;
	}

	public static int[] toPrimitiveInt(ArrayList<Long> list)
	{
		int[] data = new int[list.size() * 2];
		for (int a = 0; a < list.size(); a++)
		{
			data[a * 2] = getId(list.get(a));
			data[a * 2 + 1] = getMeta(list.get(a));
			;
		}
		return data;
	}

	public static void addPrimitiveLong(ArrayList<Long> list, long[] data)
	{
		for (int a = 0; a < data.length; a++)
		{
			list.add(data[a]);
		}
	}

	public static void addPrimitiveInt(ArrayList<Long> list, int[] data)
	{
		for (int a = 0; a < data.length / 2; a++)
		{
			list.add(conjoin(data[a * 2], data[a * 2 + 1]));
		}
	}

}
