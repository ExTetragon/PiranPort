package com.lex0v0.piranport.blockentity;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.Config;
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

// 管理二格共享储量、独立存档和稳定的流体能力代理。
public class FuelBarrelBlockEntity extends BlockEntity {
    private final FluidTank tank = new FluidTank(Config.FUEL_BARREL_CAPACITY.get()) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };
    private final IFluidHandler handler = new SharedHandler();
    @Nullable
    private BlockPos partnerPos;
    private long placeOrder = -1;
    private boolean axialConnectionAllowed = Config.FUEL_BARREL_AXIAL_CONNECTION.get();

    public FuelBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_BARREL.get(), pos, state);
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        return handler;
    }

    // 每次重新放置读取当前开关，不继承物品携带的旧方块实体策略。
    public void initializePlacement() {
        partnerPos = null;
        placeOrder = -1;
        axialConnectionAllowed = Config.FUEL_BARREL_AXIAL_CONNECTION.get();
        setChanged();
        refreshConnection();
    }

    public FluidStack getStoredFluid() {
        return handler.getFluidInTank(0);
    }

    // 仅更新本格储量，配置降容后的超额流体仍可抽出。
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

    // 只访问已加载邻块，避免查询触发区块加载。
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

    // 竖直以下桶为主，水平按放置顺序并用坐标打破同 tick 平局。
    private static FuelBarrelBlockEntity chooseMaster(FuelBarrelBlockEntity a, FuelBarrelBlockEntity b) {
        if (a.getBlockState().getValue(FuelBarrelBlock.AXIS) == Direction.Axis.Y) {
            return a.worldPosition.getY() < b.worldPosition.getY() ? a : b;
        }
        if (a.placeOrder != b.placeOrder) return a.placeOrder < b.placeOrder ? a : b;
        return a.worldPosition.compareTo(b.worldPosition) <= 0 ? a : b;
    }

    // 保留有效配对，仅与空闲且兼容的同轴邻桶连接。
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
        if (!axialConnectionAllowed) return;
        Direction.Axis axis = getBlockState().getValue(FuelBarrelBlock.AXIS);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            FuelBarrelBlockEntity other = loadedBarrel(worldPosition.relative(direction));
            if (other == null || !other.axialConnectionAllowed || !adjacentOnAxis(other) || other.partnerPos != null
                    || !compatible(tank.getFluid(), other.tank.getFluid())
                    || (long) tank.getFluidAmount() + other.tank.getFluidAmount() > 2L * Config.FUEL_BARREL_CAPACITY.get()) continue;
            if (other.placeOrder < 0) other.placeOrder = level.getGameTime();
            partnerPos = other.worldPosition;
            other.partnerPos = worldPosition;
            FuelBarrelBlockEntity master = chooseMaster(this, other);
            rebalance(master, master == this ? other : this);
            connectionChanged();
            other.connectionChanged();
            ModLog.machine(ModLog.Event.CONNECT, this, "partner", other.worldPosition.toShortString(), "master", master.worldPosition.toShortString());
            return;
        }
    }

    // 按优先级分摊兼容流体，保持总量和组件不变。
    private static void rebalance(FuelBarrelBlockEntity preferred, FuelBarrelBlockEntity other) {
        FluidStack a = preferred.tank.getFluid();
        FluidStack b = other.tank.getFluid();
        long total = (long) a.getAmount() + b.getAmount();
        int capacity = Config.FUEL_BARREL_CAPACITY.get();
        if (!compatible(a, b) || total - Math.min(capacity, total) > Integer.MAX_VALUE) return;
        int first = (int) Math.min(capacity, total);
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

    // 采集前优先填满留下的桶，余量随本格掉落。
    public void prepareForPlayerRemoval() {
        FuelBarrelBlockEntity other = getPartner();
        if (other != null) rebalance(other, this);
        disconnect();
    }

    // 仅断开配对，避免战利品生成后再次转移流体。
    public void disconnect() {
        FuelBarrelBlockEntity other = partnerPos == null ? null : loadedBarrel(partnerPos);
        if (partnerPos != null) ModLog.machine(ModLog.Event.DISCONNECT, this, "partner", partnerPos.toShortString());
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

    // 连接变化同步更新比较器并使外部能力缓存失效。
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
        tag.putLong("PlaceOrder", placeOrder);
        tag.putBoolean("AxialConnectionAllowed", axialConnectionAllowed);
        if (partnerPos != null) tag.putLong("Partner", partnerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 配置降容不截断已存流体。
        tank.setFluid(FluidStack.parseOptional(registries, tag.getCompound("Fluid")));
        placeOrder = tag.getLong("PlaceOrder");
        partnerPos = tag.contains("Partner") ? BlockPos.of(tag.getLong("Partner")) : null;
        axialConnectionAllowed = tag.getBoolean("AxialConnectionAllowed");
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent stored = input.get(ModDataComponents.FLUID_CONTENT.get());
        // 在手中物品消耗前恢复本格，连接稍后建立。
        tank.setFluid(stored == null ? FluidStack.EMPTY : stored.copy());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) components.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    // 保持代理对象稳定，每次调用重新解析当前伙伴。
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
            // 配置降容后的超额存量允许抽出，灌入仍受正常容量限制。
            int capacity = Config.FUEL_BARREL_CAPACITY.get();
            return Math.max(getPartner() == null ? capacity : 2 * capacity,
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
            int perBlock = Config.FUEL_BARREL_CAPACITY.get();
            int capacity = other == null ? perBlock : 2 * perBlock;
            int accepted = Math.min(resource.getAmount(), Math.max(0, capacity - getFluidInTank(0).getAmount()));
            if (accepted == 0 || action.simulate()) return accepted;
            FuelBarrelBlockEntity master = other == null ? FuelBarrelBlockEntity.this : getMaster();
            FuelBarrelBlockEntity second = other == null ? null : master == FuelBarrelBlockEntity.this ? other : FuelBarrelBlockEntity.this;
            // 原生 FluidTank 向超额桶灌入会截断存量，必须先跳过已满的本格。
            int filled = fillLocal(master, resource, accepted, perBlock);
            if (second != null && accepted > filled) filled += fillLocal(second, resource, accepted - filled, perBlock);
            if (filled > 0) ModLog.aggregate(ModLog.Event.BARREL_FILL, filled, true);
            return filled;
        }

        private int fillLocal(FuelBarrelBlockEntity target, FluidStack resource, int amount, int capacity) {
            if (target.tank.getFluidAmount() >= capacity) return 0;
            target.tank.setCapacity(capacity);
            return target.tank.fill(resource.copyWithAmount(amount), FluidAction.EXECUTE);
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
                ModLog.aggregate(ModLog.Event.BARREL_DRAIN, amount, true);
            }
            return result;
        }
    }
}
