package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hbm.handler.FluidTypeHandler.FluidType;
import com.hbm.interfaces.IFluidAcceptor;
import com.hbm.interfaces.IFluidContainer;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.FluidTank;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.AuxGaugePacket;
import com.hbm.packet.PacketDispatcher;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyGenerator;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityMachineSeleniumEngine extends TileEntity implements ISidedInventory, IEnergyGenerator, IFluidContainer, IFluidAcceptor {

	private ItemStack slots[];

	public long power;
	public int soundCycle = 0;
	public static final long maxPower = 250000;
	public long powerCap = 250000;
	public FluidTank tank;
	public int pistonCount = 0;

	private static final int[] slots_top = new int[] { 0 };
	private static final int[] slots_bottom = new int[] { 1, 2 };
	private static final int[] slots_side = new int[] { 2 };

	private String customName;

	public TileEntityMachineSeleniumEngine() {
		slots = new ItemStack[14];
		tank = new FluidTank(FluidType.DIESEL, 16000, 0);
	}

	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		if (slots[i] != null) {
			ItemStack itemStack = slots[i];
			slots[i] = null;
			return itemStack;
		} else {
			return null;
		}
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemStack) {
		slots[i] = itemStack;
		if (itemStack != null && itemStack.stackSize > getInventoryStackLimit()) {
			itemStack.stackSize = getInventoryStackLimit();
		}
	}

	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.machineSelenium";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		} else {
			return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64;
		}
	}

	// You scrubs aren't needed for anything (right now)
	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if (i == 9)
			if (FluidContainerRegistry.getFluidContent(stack, tank.getTankType()) > 0)
				return true;
		if (i == 13)
			if (stack.getItem() instanceof IBatteryItem)
				return true;

		return false;
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if (slots[i] != null) {
			if (slots[i].stackSize <= j) {
				ItemStack itemStack = slots[i];
				slots[i] = null;
				return itemStack;
			}
			ItemStack itemStack1 = slots[i].splitStack(j);
			if (slots[i].stackSize == 0) {
				slots[i] = null;
			}

			return itemStack1;
		} else {
			return null;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagList list = nbt.getTagList("items", 10);

		this.power = nbt.getLong("powerTime");
		this.powerCap = nbt.getLong("powerCap");
		tank.readFromNBT(nbt, "fuel");
		slots = new ItemStack[getSizeInventory()];

		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			byte b0 = nbt1.getByte("slot");
			if (b0 >= 0 && b0 < slots.length) {
				slots[b0] = ItemStack.loadItemStackFromNBT(nbt1);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("powerTime", power);
		nbt.setLong("powerCap", powerCap);
		tank.writeToNBT(nbt, "fuel");
		NBTTagList list = new NBTTagList();

		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null) {
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setByte("slot", (byte) i);
				slots[i].writeToNBT(nbt1);
				list.appendTag(nbt1);
			}
		}
		nbt.setTag("items", list);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int p_94128_1_) {
		return p_94128_1_ == 0 ? slots_bottom : (p_94128_1_ == 1 ? slots_top : slots_side);
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemStack, int j) {
		return this.isItemValidForSlot(i, itemStack);
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		if (i == 1)
			if (itemStack.getItem() == ModItems.canister_empty || itemStack.getItem() == ModItems.tank_steel)
				return true;
		if (i == 2)
			if (itemStack.getItem() instanceof IBatteryItem && ((IBatteryItem)itemStack.getItem()).getCharge(itemStack) == ((IBatteryItem)itemStack.getItem()).getMaxCharge())
				return true;

		return false;
	}

	public long getPowerScaled(long i) {
		return (power * i) / powerCap;
	}

	@Override
	public void updateEntity() {
		
		if (!worldObj.isRemote) {
			
			this.sendPower(worldObj, xCoord, yCoord - 1, zCoord, ForgeDirection.DOWN);
			
			pistonCount = countPistons();

			//Tank Management
			tank.setType(11, 12, slots);
			tank.loadTank(9, 10, slots);
			tank.updateTank(xCoord, yCoord, zCoord, worldObj.provider.dimensionId);

			FluidType type = tank.getTankType();
			if(type.name().equals(FluidType.NITAN.name()))
				powerCap = maxPower * 10;
			else
				powerCap = maxPower;
			
			// Battery Item
			power = Library.chargeItemsFromTE(slots, 13, power, powerCap);

			if(this.pistonCount > 2)
				generate();

			PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(xCoord, yCoord, zCoord, power), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 50));
			PacketDispatcher.wrapper.sendToAllAround(new AuxGaugePacket(xCoord, yCoord, zCoord, pistonCount, 0), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 150));
			PacketDispatcher.wrapper.sendToAllAround(new AuxGaugePacket(xCoord, yCoord, zCoord, (int)powerCap, 1), new TargetPoint(worldObj.provider.dimensionId, xCoord, yCoord, zCoord, 50));
		}
	}
	
	public int countPistons() {
		int count = 0;
		
		for(int i = 0; i < 9; i++) {
			if(slots[i] != null && slots[i].getItem() == ModItems.piston_selenium)
				count++;
		}
		
		return count;
	}
	
	public boolean hasAcceptableFuel() {
		return getHEFromFuel() > 0;
	}
	
	public static final HashMap<FluidType, Integer> fuels = new HashMap();
	
	static {
		fuels.put(FluidType.SMEAR,		50);
		fuels.put(FluidType.HEATINGOIL,	75);
		fuels.put(FluidType.HYDROGEN,	5);
		fuels.put(FluidType.DIESEL,		225);
		fuels.put(FluidType.KEROSENE,	300);
		fuels.put(FluidType.RECLAIMED,	100);
		fuels.put(FluidType.PETROIL,	125);
		fuels.put(FluidType.BIOFUEL,	200);
		fuels.put(FluidType.GASOLINE,	700);
		fuels.put(FluidType.NITAN,		2500);
		fuels.put(FluidType.LPG,		200);
		fuels.put(FluidType.ETHANOL,	75);
	}
	
	public int getHEFromFuel() {
		FluidType type = tank.getTankType();
		Integer value = fuels.get(type);
		return value != null ? value : null;
	}

	public void generate() {
		if (hasAcceptableFuel()) {
			if (tank.getFill() > 0) {
				if (soundCycle == 0) {
					//if (tank.getTankType().name().equals(FluidType.) > 0)
					//	this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "fireworks.blast", 1.0F, 1.0F);
					//else
						this.worldObj.playSoundEffect(this.xCoord, this.yCoord, this.zCoord, "fireworks.blast", 1.0F, 0.5F);
				}
				soundCycle++;

				if (soundCycle >= 3)
					soundCycle = 0;

				tank.setFill(tank.getFill() - this.pistonCount);
				if(tank.getFill() < 0)
					tank.setFill(0);

				power += getHEFromFuel() * Math.pow(this.pistonCount, 1.15D);
					
				if(power > powerCap)
					power = powerCap;
			}
		}
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public void setPower(long i) {
		this.power = i;
	}

	@Override
	public void setFillstate(int fill, int index) {
		tank.setFill(fill);
	}

	@Override
	public void setType(FluidType type, int index) {
		tank.setTankType(type);
	}

	@Override
	public int getMaxFluidFill(FluidType type) {
		return type.name().equals(this.tank.getTankType().name()) ? tank.getMaxFill() : 0;
	}

	@Override
	public int getFluidFill(FluidType type) {
		return type.name().equals(this.tank.getTankType().name()) ? tank.getFill() : 0;
	}

	@Override
	public void setFluidFill(int i, FluidType type) {
		if(type.name().equals(tank.getTankType().name()))
			tank.setFill(i);
	}

	@Override
	public List<FluidTank> getTanks() {
		List<FluidTank> list = new ArrayList();
		list.add(tank);
		
		return list;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir == ForgeDirection.DOWN;
	}
}
