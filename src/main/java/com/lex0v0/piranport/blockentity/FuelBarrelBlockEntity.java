package com.lex0v0.piranport.blockentity;

import com.lex0v0.piranport.block.FuelBarrelBlock;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.lex0v0.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** 两格逻辑共享、每格独立持久化。稳定的代理不会留下指向旧 master 的管道引用。 */
public class FuelBarrelBlockEntity extends BlockEntity {
    public static final int CAPACITY_PER_BLOCK = 32_000;
    private final FluidTank tank = new FluidTank(CAPACITY_PER_BLOCK) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final IFluidHandler handler = new SharedHandler();
    @Nullable
    private BlockPos partnerPos;
    private long placeOrder = -1;

    public FuelBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_BARREL.get(), pos, state);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return handler;
    }

    public FluidStack getStoredFluid() {
        return handler.getFluidInTank(0);
    }

    /** 只写本格，不覆盖邻桶；旧版超额存量完整保留，等待配对分摊或抽出。 */
    public void setStoredFluid(FluidStack stack) {
        tank.setFluid(stack.copy());
        contentsChanged();
    }

    public int getComparatorOutput() {
        return Math.min(15, (int) (15L * getStoredFluid().getAmount() / handler.getTankCapacity(0)));
    }

    public FuelBarrelBlockEntity getMaster() {
        FuelBarrelBlockEntity other = getPartner();
        return other == null ? this : chooseMaster(this, other);
    }

    @Nullable
    private FuelBarrelBlockEntity loadedBarrel(BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof FuelBarrelBlockEntity other && !other.isRemoved() ? other : null;
    }

    @Nullable
    private FuelBarrelBlockEntity getPartner() {
        if (partnerPos == null || isRemoved()) return null;
        FuelBarrelBlockEntity other = loadedBarrel(partnerPos);
        return other != null && worldPosition.equals(other.partnerPos) && adjacentOnAxis(other)
                && compatible(tank.getFluid(), other.tank.getFluid()) ? other : null;
    }

    private boolean adjacentOnAxis(FuelBarrelBlockEntity other) {
        Direction.Axis axis = getBlockState().getValue(FuelBarrelBlock.AXIS);
        if (other.getBlockState().getValue(FuelBarrelBlock.AXIS) != axis) return false;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis && worldPosition.relative(direction).equals(other.worldPosition)) return true;
        }
        return false;
    }

    private static boolean compatible(FluidStack a, FluidStack b) {
        return a.isEmpty() || b.isEmpty() || FluidStack.isSameFluidSameComponents(a, b);
    }

    private static FuelBarrelBlockEntity chooseMaster(FuelBarrelBlockEntity a, FuelBarrelBlockEntity b) {
        if (a.getBlockState().getValue(FuelBarrelBlock.AXIS) == Direction.Axis.Y) {
            return a.worldPosition.getY() < b.worldPosition.getY() ? a : b;
        }
        if (a.placeOrder != b.placeOrder) return a.placeOrder < b.placeOrder ? a : b;
        return a.worldPosition.compareTo(b.worldPosition) <= 0 ? a : b;
    }

    /** 已有配对保持不变；第三格只能选择未配对且油品兼容的邻桶。 */
    public void refreshConnection() {
        if (level == null || level.isClientSide || isRemoved()) return;
        if (placeOrder < 0) {
            placeOrder = level.getGameTime();
            setChanged();
        }
        if (partnerPos != null) {
            if (!level.hasChunkAt(partnerPos)) return; // 不强制加载邻块，也不抹掉未加载的伙伴关系。
            FuelBarrelBlockEntity other = getPartner();
            if (other != null) {
                FuelBarrelBlockEntity master = chooseMaster(this, other);
                rebalance(master, master == this ? other : this);
                return;
            }
            disconnect();
        }
        Direction.Axis axis = getBlockState().getValue(FuelBarrelBlock.AXIS);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            FuelBarrelBlockEntity other = loadedBarrel(worldPosition.relative(direction));
            if (other == null || !adjacentOnAxis(other) || other.partnerPos != null
                    || !compatible(tank.getFluid(), other.tank.getFluid())
                    || (long) tank.getFluidAmount() + other.tank.getFluidAmount() > 2L * CAPACITY_PER_BLOCK) continue;
            if (other.placeOrder < 0) other.placeOrder = level.getGameTime();
            partnerPos = other.worldPosition;
            other.partnerPos = worldPosition;
            FuelBarrelBlockEntity master = chooseMaster(this, other);
            rebalance(master, master == this ? other : this);
            connectionChanged();
            other.connectionChanged();
            return;
        }
    }

    /** 先检查总量和组件兼容性，再完整分配；不截断、不重建丢失组件的流体。 */
    private static void rebalance(FuelBarrelBlockEntity preferred, FuelBarrelBlockEntity other) {
        FluidStack a = preferred.tank.getFluid();
        FluidStack b = other.tank.getFluid();
        long total = (long) a.getAmount() + b.getAmount();
        if (!compatible(a, b) || total > 2L * CAPACITY_PER_BLOCK) return;
        int first = (int) Math.min(CAPACITY_PER_BLOCK, total);
        int second = (int) total - first;
        if (a.getAmount() == first && b.getAmount() == second) return;
        FluidStack fluid = a.isEmpty() ? b : a;
        preferred.tank.setFluid(first == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(first));
        other.tank.setFluid(second == 0 ? FluidStack.EMPTY : fluid.copyWithAmount(second));
        preferred.contentsChanged();
        other.contentsChanged();
    }

    public void detachForAxisChange() {
        FuelBarrelBlockEntity other = getPartner();
        if (other != null) {
            FuelBarrelBlockEntity master = chooseMaster(this, other);
            rebalance(master, master == this ? other : this);
        }
        disconnect();
    }

    /** 玩家采集前让留下的桶优先装满，其余流体通过本格的战利品组件带走。 */
    public void prepareForPlayerRemoval() {
        FuelBarrelBlockEntity other = getPartner();
        if (other != null) rebalance(other, this);
        disconnect();
    }

    /** 爆炸可能已生成战利品，这里只断开关系，不再次搬移或结算流体。 */
    public void disconnect() {
        FuelBarrelBlockEntity other = partnerPos == null ? null : loadedBarrel(partnerPos);
        partnerPos = null;
        if (other != null && worldPosition.equals(other.partnerPos)) {
            other.partnerPos = null;
            other.connectionChanged();
            other.scheduleConnectionRefresh();
        }
        connectionChanged();
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            FuelBarrelBlockEntity other = getPartner();
            if (other != null) level.updateNeighbourForOutputSignal(other.worldPosition, other.getBlockState().getBlock());
        }
    }

    private void connectionChanged() {
        contentsChanged();
        if (level != null && !level.isClientSide) level.invalidateCapabilities(worldPosition);
    }

    public void scheduleConnectionRefresh() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleConnectionRefresh();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", tank.getFluid().saveOptional(registries));
        tag.putInt("StorageVersion", 2);
        tag.putLong("PlaceOrder", placeOrder);
        if (partnerPos != null) tag.putLong("Partner", partnerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 兼容旧版 Fluid/PlaceOrder；旧版 master 的 64000 mB 先完整读入，再分摊。
        tank.setFluid(FluidStack.parseOptional(registries, tag.getCompound("Fluid")));
        placeOrder = tag.contains("PlaceOrder") ? tag.getLong("PlaceOrder") : -1;
        partnerPos = tag.contains("Partner") ? BlockPos.of(tag.getLong("Partner")) : null;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent stored = input.get(ModDataComponents.FLUID_CONTENT.get());
        // vanilla 在消耗手中物品之前调用，仅恢复本格，然后才建立连接。
        tank.setFluid(stored == null ? FluidStack.EMPTY : stored.copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) components.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    private final class SharedHandler implements IFluidHandler {
        private boolean usable() {
            return level != null && !level.isClientSide && !isRemoved();
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int index) {
            if (!usable()) return FluidStack.EMPTY;
            FuelBarrelBlockEntity other = getPartner();
            FluidStack fluid = tank.getFluid();
            if (other == null) return fluid.copy();
            FluidStack second = other.tank.getFluid();
            return (fluid.isEmpty() ? second : fluid).copyWithAmount(fluid.getAmount() + second.getAmount());
        }

        @Override
        public int getTankCapacity(int index) {
            // 旧版孤立超额存量可继续抽出，但 fill 仍采用正常上限，不能继续灌入。
            return Math.max(getPartner() == null ? CAPACITY_PER_BLOCK : 2 * CAPACITY_PER_BLOCK,
                    getFluidInTank(index).getAmount());
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return compatible(getFluidInTank(index), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!usable() || resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            FuelBarrelBlockEntity other = getPartner();
            int capacity = other == null ? CAPACITY_PER_BLOCK : 2 * CAPACITY_PER_BLOCK;
            int accepted = Math.min(resource.getAmount(), Math.max(0, capacity - getFluidInTank(0).getAmount()));
            if (accepted == 0 || action.simulate()) return accepted;
            FuelBarrelBlockEntity master = other == null ? FuelBarrelBlockEntity.this : getMaster();
            FuelBarrelBlockEntity second = other == null ? null : master == FuelBarrelBlockEntity.this ? other : FuelBarrelBlockEntity.this;
            int first = master.tank.fill(resource.copyWithAmount(accepted), action);
            if (second != null && accepted > first) second.tank.fill(resource.copyWithAmount(accepted - first), action);
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, getFluidInTank(0))
                    ? FluidStack.EMPTY : drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack stored = getFluidInTank(0);
            int amount = Math.min(Math.max(0, maxDrain), stored.getAmount());
            if (amount == 0) return FluidStack.EMPTY;
            FluidStack result = stored.copyWithAmount(amount);
            if (action.execute()) {
                FuelBarrelBlockEntity other = getPartner();
                FuelBarrelBlockEntity master = other == null ? FuelBarrelBlockEntity.this : getMaster();
                FuelBarrelBlockEntity second = other == null ? null : master == FuelBarrelBlockEntity.this ? other : FuelBarrelBlockEntity.this;
                int drained = second == null ? 0 : second.tank.drain(amount, action).getAmount();
                master.tank.drain(amount - drained, action);
                scheduleConnectionRefresh();
            }
            return result;
        }
    }
}
