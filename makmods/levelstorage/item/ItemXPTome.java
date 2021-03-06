package makmods.levelstorage.item;

import java.util.List;
import java.util.Random;

import makmods.levelstorage.LSBlockItemList;
import makmods.levelstorage.LevelStorage;
import makmods.levelstorage.gui.logicslot.LogicSlot;
import makmods.levelstorage.init.Config;
import makmods.levelstorage.init.IHasRecipe;
import makmods.levelstorage.init.Config.LSConfigCategory;
import makmods.levelstorage.logic.ExperienceRecipe;
import makmods.levelstorage.logic.util.CommonHelper;
import makmods.levelstorage.logic.util.NBTHelper;
import makmods.levelstorage.logic.util.NBTHelper.Cooldownable;
import makmods.levelstorage.proxy.ClientProxy;
import makmods.levelstorage.proxy.LSKeyboard;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemXPTome extends Item implements IHasRecipe {

	private int bookMaxStorage;
	public static final int COOLDOWN = 3;

	public static String STORED_XP_NBT = "storedXP";
	public static int xpPerInteraction = 100;
	public static int XP_PER_BOOKCRAFT = Config.getInt(LSConfigCategory.BALANCE, "xpPerBookEnchantment", 840, "Determines how much XP is consumed from XP tome when you try to enchant a book with it (default V while holding XP tome in your hand and having books in inventory");

	public ItemXPTome(int id) {
		super(id);
		this.bookMaxStorage = LevelStorage.itemLevelStorageBookSpace;
		this.setMaxDamage(512);
		this.setNoRepair();
		this.setCreativeTab(CreativeTabs.tabTools);
		this.setMaxStackSize(1);
	}

	public void addCraftingRecipe() {
		// Book
		ItemStack stackDepleted = new ItemStack(
				LSBlockItemList.itemLevelStorageBook, 1, 0);
		stackDepleted.stackTagCompound = new NBTTagCompound();
		ItemStack stackBook = new ItemStack(Item.book);
		ItemStack stackGoldBlock = new ItemStack(Block.blockGold);
		ItemStack stackEnchTable = new ItemStack(Block.enchantmentTable);
		GameRegistry.addShapelessRecipe(stackDepleted, stackBook,
				stackGoldBlock, stackEnchTable);
		if (LevelStorage.experienceRecipesOn) {
			CraftingManager.getInstance().getRecipeList()
					.add(new ExperienceRecipe());
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World,
			EntityPlayer par3EntityPlayer) {
		if (!par2World.isRemote) {
			if (!Cooldownable.use(par1ItemStack, COOLDOWN))
				return par1ItemStack;
			if (par3EntityPlayer.isSneaking()) {
				if ((this.bookMaxStorage - getStoredXP(par1ItemStack)) > par3EntityPlayer.experienceTotal) {
					increaseStoredXP(par1ItemStack,
							par3EntityPlayer.experienceTotal);
					par3EntityPlayer.experienceTotal = 0;
					par3EntityPlayer.experienceLevel = 0;
					par3EntityPlayer.experience = 0;
					// par3EntityPlayer.setScore(0);
				}
			} else {
				xpPerInteraction = par3EntityPlayer.xpBarCap();
				if (getStoredXP(par1ItemStack) > xpPerInteraction) {
					par3EntityPlayer.addExperience(xpPerInteraction);
					increaseStoredXP(par1ItemStack, -xpPerInteraction);
					float f = 5 / 30.0F;
					par2World.playSoundAtEntity(par3EntityPlayer,
							"random.levelup", f * 0.75F, 1.0F);
				} else {
					if (getStoredXP(par1ItemStack) != 0) {
						par3EntityPlayer
								.addExperience(getStoredXP(par1ItemStack));
						par1ItemStack.stackTagCompound.setInteger(
								STORED_XP_NBT, 0);
						float f = 5 / 30.0F;
						par2World.playSoundAtEntity(par3EntityPlayer,
								"random.levelup", f * 0.75F, 1.0F);
					}
				}
			}
		}
		return par1ItemStack;
	}

	public static void increaseStoredXP(ItemStack stack, int amount) {
		NBTHelper.checkNBT(stack);
		NBTHelper.decreaseIntegerIgnoreZero(stack, STORED_XP_NBT, -1 * amount);
	}

	public static int getStoredXP(ItemStack stack) {
		NBTHelper.checkNBT(stack);
		return NBTHelper.getInteger(stack, STORED_XP_NBT);
	}

	public ItemStack generateRandomBook(Random rand) {
		List<Enchantment> availableIDs = Lists.newArrayList();
		for (Enchantment ench : Enchantment.enchantmentsBookList)
			if (ench != null)
				availableIDs.add(ench);
		int idToSelect = rand.nextInt(availableIDs.size());
		Enchantment selected = availableIDs.get(idToSelect);
		int level = selected.getMaxLevel() == 1 ? 1 : rand.nextInt(selected
				.getMaxLevel() - 1) + 1;
		ItemStack enchantedBook = CommonHelper.createEnchantedBook(selected,
				level);
		return enchantedBook;
	}

	public void attemptCreateEnchBook(ItemStack xpTome, World par2World,
			EntityPlayer player) {
		if (!(getStoredXP(xpTome) >= XP_PER_BOOKCRAFT))
			return;
		LogicSlot[] playerSlots = new LogicSlot[player.inventory.mainInventory.length];
		for (int i = 0; i < player.inventory.mainInventory.length; i++)
			playerSlots[i] = new LogicSlot(player.inventory, i);
		LogicSlot booksSlot = null;
		for (LogicSlot slot : playerSlots) {
			if (slot.get() != null)
				if (slot.get().getItem() instanceof ItemBook) {
					booksSlot = slot;
					break;
				}
		}
		if (booksSlot != null && booksSlot.get() != null) {
			if (booksSlot.canConsume(1)) {
				boolean shouldConsume = false;
				for (LogicSlot slot : playerSlots) {
					if (slot.get() == null && slot.add(generateRandomBook(par2World.rand), false)) {
						shouldConsume = true;
						break;
					}
				}
				if (shouldConsume) {
					increaseStoredXP(xpTome, -XP_PER_BOOKCRAFT);
					booksSlot.consume(1);
				}
			}
		}
	}

	@Override
	public void onUpdate(ItemStack par1ItemStack, World par2World,
			Entity par3Entity, int par4, boolean par5) {
		if (!par2World.isRemote) {
			Cooldownable.onUpdate(par1ItemStack, COOLDOWN);
			this.setDamage(par1ItemStack, calculateDurability(par1ItemStack));
			if (par3Entity instanceof EntityPlayerMP) {
				EntityPlayer player = (EntityPlayer)par3Entity;
				if (player.inventory.getCurrentItem() != par1ItemStack)
					return;
				if (LSKeyboard.getInstance().isKeyDown(
						(EntityPlayer) par3Entity, LSKeyboard.RANGE_KEY_NAME)) {
					if (Cooldownable.use(par1ItemStack, COOLDOWN * 4)) {
						attemptCreateEnchBook(par1ItemStack, par2World,
								(EntityPlayer) par3Entity);
					}
				}
			}
		}
	}

	@Override
	public void addInformation(ItemStack par1ItemStack,
			EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
		// Here we add our nice little tooltip
		super.addInformation(par1ItemStack, par2EntityPlayer, par3List, par4);
		NBTHelper.checkNBT(par1ItemStack);
		par3List.add(StatCollector.translateToLocal("tooltip.xptome.stored")
				+ " " + String.valueOf(getStoredXP(par1ItemStack)) + " "
				+ StatCollector.translateToLocal("tooltip.xptome.xppoints"));
	}

	public static int calculateDurability(ItemStack stack) {
		float percent = ((getStoredXP(stack) * 100.0f) / LevelStorage.itemLevelStorageBookSpace) / 100;
		int durability = stack.getMaxDamage()
				- (int) (stack.getMaxDamage() * percent);
		if (durability == 0) {
			durability = 1;
		}
		return durability;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return EnumRarity.rare;
	}

	@Override
	public void getSubItems(int par1, CreativeTabs par2CreativeTabs,
			List par3List) {
		ItemStack stackFull = new ItemStack(par1, 1, 1);
		stackFull.stackTagCompound = new NBTTagCompound();
		stackFull.stackTagCompound.setInteger(STORED_XP_NBT,
				this.bookMaxStorage - 1);
		ItemStack stackDepleted = new ItemStack(par1, 1,
				this.getMaxDamage() - 1);
		stackDepleted.stackTagCompound = new NBTTagCompound();
		stackDepleted.stackTagCompound.setInteger(STORED_XP_NBT, 0);
		par3List.add(stackFull);
		par3List.add(stackDepleted);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister par1IconRegister) {
		this.itemIcon = par1IconRegister.registerIcon(ClientProxy.BOOK_TEXTURE);
	}
}
