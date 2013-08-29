package makmods.levelstorage;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A simple creative tab all my stuff is located on
 * @author mak326428
 *
 */
public class LevelStorageCreativeTab extends CreativeTabs {
	
	@SideOnly(Side.CLIENT)
	public static LevelStorageCreativeTab instance;

	public LevelStorageCreativeTab() {
		super("levelstorage");
	}

	public ItemStack getIconItemStack() {
		return new ItemStack(LSBlockItemList.itemLevitationBoots);
	}
}
