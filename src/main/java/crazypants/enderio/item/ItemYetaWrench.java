package crazypants.enderio.item;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

import com.enderio.core.api.client.gui.IAdvancedTooltipProvider;
import com.enderio.core.client.handlers.SpecialTooltipHandler;

import crazypants.enderio.EnderIO;
import crazypants.enderio.EnderIOTab;
import crazypants.enderio.ModObject;
import crazypants.enderio.api.tool.IConduitControl;
import crazypants.enderio.api.tool.ITool;
import crazypants.enderio.conduit.ConduitDisplayMode;
import crazypants.enderio.config.Config;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.paint.PainterUtil2;
import crazypants.enderio.paint.YetaUtil;
import crazypants.enderio.paint.IPaintable.IBlockPaintableBlock;
import crazypants.enderio.tool.ToolUtil;

public class ItemYetaWrench extends Item implements ITool, IConduitControl, IAdvancedTooltipProvider, InvocationHandler {

  public static ItemYetaWrench create() {
    if (Config.useSneakMouseWheelYetaWrench) {
      PacketHandler.INSTANCE.registerMessage(YetaWrenchPacketProcessor.class, YetaWrenchPacketProcessor.class, PacketHandler.nextID(), Side.SERVER);
    }
    ItemYetaWrench result = new ItemYetaWrench();
    result = ToolUtil.addInterfaces(result);

    GameRegistry.registerItem(result, ModObject.itemYetaWrench.unlocalisedName);

    return result;
  }

  protected ItemYetaWrench() {
    setCreativeTab(EnderIOTab.tabEnderIO);
    setUnlocalizedName(ModObject.itemYetaWrench.unlocalisedName);
    setMaxStackSize(1);
  }

  @Override  
  public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
    final IBlockState blockState = world.getBlockState(pos);
    IBlockState bs = blockState;
    Block block = bs.getBlock();
    boolean ret = false;
    if (block != null) {
      PlayerInteractEvent e = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, pos, side, world, new Vec3(hitX, hitY, hitZ));
      if (MinecraftForge.EVENT_BUS.post(e) || e.getResult() == Result.DENY || e.useBlock == Result.DENY || e.useItem == Result.DENY) {
        return false;
      }
      if (!player.isSneaking() && block.rotateBlock(world, pos, side)) {
        if (block == Blocks.chest) {
          // This works around a forge bug where you can rotate double chests to invalid directions
          TileEntityChest te = (TileEntityChest) world.getTileEntity(pos);
          if (te.adjacentChestXNeg != null || te.adjacentChestXPos != null || te.adjacentChestZNeg != null || te.adjacentChestZPos != null) {
            // Render master is always the chest to the negative direction
            TileEntityChest masterChest = te.adjacentChestXNeg == null && te.adjacentChestZNeg == null ? te : te.adjacentChestXNeg == null ? te.adjacentChestZNeg: te.adjacentChestXNeg;
            if (masterChest != te) {
              //TODO: 1.8
//              int meta = world.getBlockMetadata(masterChest.xCoord, masterChest.yCoord, masterChest.zCoord);
//              world.setBlockMetadataWithNotify(masterChest.xCoord, masterChest.yCoord, masterChest.zCoord, meta ^ 1, 3);
            } else {
              // If this is the master chest, we can just rotate twice
              block.rotateBlock(world,pos, side);
            }
          }
        }
        ret = true;
      } else if (block instanceof IBlockPaintableBlock && !player.isSneaking() && !YetaUtil.shouldHeldItemHideFacades()) {
        IBlockState paintSource = ((IBlockPaintableBlock) block).getPaintSource(blockState, world, pos);
        if (paintSource != null) {
          final IBlockState rotatedPaintSource = PainterUtil2.rotate(paintSource);
          if (rotatedPaintSource != paintSource) {
            ((IBlockPaintableBlock) block).setPaintSource(blockState, world, pos, rotatedPaintSource);
          }
          ret = true;
        }
      }
    }
    if (ret) {
      player.swingItem();
    }
    return ret && !world.isRemote;
  }

  @Override
  public ItemStack onItemRightClick(ItemStack equipped, World world, EntityPlayer player) {
    if (!Config.useSneakRightClickYetaWrench) {
      return equipped;
    }
    if (!player.isSneaking()) {
      return equipped;
    }
    ConduitDisplayMode curMode = ConduitDisplayMode.getDisplayMode(equipped);
    if (curMode == null) {
      curMode = ConduitDisplayMode.ALL;
    }
    ConduitDisplayMode newMode = curMode.next();
    ConduitDisplayMode.setDisplayMode(equipped, newMode);
    return equipped;
  }

  @Override
  public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
    IBlockState bs = player.worldObj.getBlockState(pos);
    Block block = bs.getBlock();
    if (player.isSneaking() && block == EnderIO.blockConduitBundle && player.capabilities.isCreativeMode) {
      block.onBlockClicked(player.worldObj, pos, player);
      return true;
    }
    return false;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public boolean isFull3D() {
    return true;
  }

  @Override
  public boolean doesSneakBypassUse(World world, BlockPos pos, EntityPlayer player) {
    return true;
  }

  
  @Override
  public boolean canUse(ItemStack stack, EntityPlayer player, BlockPos pos) {  
    return true;
  }

  @Override
  public void used(ItemStack stack, EntityPlayer player, BlockPos pos) {
  }

  @Override
  public boolean shouldHideFacades(ItemStack stack, EntityPlayer player) {
    ConduitDisplayMode curMode = ConduitDisplayMode.getDisplayMode(stack);
    return curMode != ConduitDisplayMode.NONE;
  }

  @Override
  public boolean showOverlay(ItemStack stack, EntityPlayer player) {
    return true;
  }

  /* IAdvancedTooltipProvider */

  @Override
  public void addBasicEntries(ItemStack itemstack, EntityPlayer entityplayer, List<String> list, boolean flag) {
  }

  @Override
  public void addCommonEntries(ItemStack itemstack, EntityPlayer entityplayer, List<String> list, boolean flag) {
  }

  @Override
  public void addDetailedEntries(ItemStack itemstack, EntityPlayer entityplayer, List<String> list, boolean flag) {
    ArrayList<String> tmp = new ArrayList<String>();
    SpecialTooltipHandler.addDetailedTooltipFromResources(tmp, getUnlocalizedName());
    String keyName = Keyboard.getKeyName(KeyTracker.instance.getYetaWrenchMode().getKeyCode());
    for (String line : tmp) {
      list.add(String.format(line, keyName));
    }
  }

  /* InvocationHandler */

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    System.out.println("ItemYetaWrench.invoke: method = " + method.getName());
    return null;
  }
}
