package com.lex0v0.piranport.blockentity;

import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardBrowseState;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardJob;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.item.blueprint.BlueprintRules;
import com.lex0v0.piranport.item.blueprint.BlueprintType;
import com.lex0v0.piranport.item.rigging.RiggingCoreRules;
import com.lex0v0.piranport.menu.ShipyardMenu;
import com.lex0v0.piranport.recipe.ShipyardRecipe;
import com.lex0v0.piranport.registry.ModBlockEntities;
import com.lex0v0.piranport.registry.ModDataComponents;
import com.lex0v0.piranport.registry.ModFluids;
import com.lex0v0.piranport.registry.ModItems;
import com.lex0v0.piranport.registry.ModRecipeTypes;
import com.lex0v0.piranport.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// 单工位统一建造所有船型；资源变动与任务结算只在服务端执行。
public final class ShipyardBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FUEL_CAPACITY = 32000;
    public static final int BLUEPRINT_SLOT = 0, OUTPUT_SLOT = 1, CACHE_START = 2;
    public static final int TOTAL_SLOTS = CACHE_START + ManufacturingMaterials.SLOTS;
    private ShipyardJob job;
    private ShipyardBrowseState remembered = new ShipyardBrowseState();
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int slot) { changed(); }
        @Override public int getSlotLimit(int slot) { return slot == OUTPUT_SLOT ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
    };
    private final FluidTank tank = new FluidTank(FUEL_CAPACITY, stack -> stack.is(ModFluids.FUEL_OIL.get())) {
        @Override protected void onContentsChanged() { changed(); }
    };
    private final IFluidHandler fluidHandler = new FuelHandler();
    private final IItemHandler top = new FaceInventory(new int[]{BLUEPRINT_SLOT}, true);
    private final IItemHandler sides = new FaceInventory(java.util.stream.IntStream.range(OUTPUT_SLOT, TOTAL_SLOTS).toArray(), true);
    private final IItemHandler bottom = new FaceInventory(new int[]{OUTPUT_SLOT}, false);
    private final IItemHandlerModifiable menuInventory = new MenuInventory();
    private long revision;
    private boolean invalidStoredJob;
    private String loggedJobState;

    public ShipyardBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SHIPYARD.get(), pos, state); }
    private boolean usable() { return level != null && !level.isClientSide && !isRemoved() && level.getBlockEntity(worldPosition) == this; }
    private void changed() { revision++; setChanged(); }
    public long revision() { return revision; }
    public @Nullable ShipyardJob job() { return job; }
    public ItemStack stack(int slot) { return inventory.getStackInSlot(slot).copy(); }
    public FluidStack fuel() { return tank.getFluid().copy(); }
    public IItemHandlerModifiable menuInventory() { return menuInventory; }
    public ShipyardBrowseState remembered() { return remembered.copy(); }
    public void remember(ShipyardBrowseState state) { if (usable()) { remembered = state.copy(); changed(); } }
    public List<ItemStack> materials() { return java.util.stream.IntStream.range(CACHE_START, TOTAL_SLOTS).mapToObj(this::stack).toList(); }
    public IItemHandler items(@Nullable Direction side) {
        return side == Direction.UP ? top : side == Direction.DOWN ? bottom : sides;
    }
    public @Nullable IFluidHandler fluid(@Nullable Direction side) { return side == Direction.UP ? null : fluidHandler; }

    public boolean accepts(int slot, ItemStack stack) {
        if (slot < 0 || slot >= TOTAL_SLOTS || stack.isEmpty()) return false;
        if (slot >= CACHE_START) return ManufacturingMaterials.accepts(stack);
        if (slot == OUTPUT_SLOT) return false;
        if (!stack.is(ModItems.SHIP_BLUEPRINT.get()) || !BlueprintRules.isValid(stack)) return false;
        return job == null || ItemStack.isSameItemSameComponents(stack, job.blueprint);
    }

    public int extractable(int slot) {
        int count = inventory.getStackInSlot(slot).getCount();
        return slot == BLUEPRINT_SLOT && job != null ? Math.max(0, count - 1) : count;
    }

    public ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return usable() && accepts(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
    }

    public ItemStack extract(int slot, int count, boolean simulate) {
        return usable() ? inventory.extractItem(slot, Math.min(Math.max(count, 0), extractable(slot)), simulate) : ItemStack.EMPTY;
    }

    public @Nullable ShipyardRecipe recipe(ResourceLocation ship) {
        if (level == null || ship == null) return null;
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SHIPYARD.get()).stream()
                .filter(holder -> holder.value().ship().equals(ship)).sorted(Comparator.comparing(holder -> holder.id().toString()))
                .map(holder -> holder.value()).findFirst().orElse(null);
    }

    public boolean permitted(ResourceLocation ship) {
        var definition = ShipDefinitions.byId(ship);
        return definition != null && BlueprintRules.allowsType(inventory.getStackInSlot(BLUEPRINT_SLOT), BlueprintType.byId(definition.type().id()));
    }

    public boolean affordable(ShipyardRecipe recipe) {
        return recipe != null && (recipe.fuel() == 0 || tank.getFluid().is(ModFluids.FUEL_OIL.get()))
                && tank.getFluidAmount() >= recipe.fuel() && ManufacturingMaterials.plan(materials(), recipe) != null;
    }

    public enum StartResult { STARTED, BUSY, OUTPUT_PENDING, INVALID_BLUEPRINT, MISSING_RESOURCES, NO_RECIPE }

    public StartResult start(ResourceLocation ship, @Nullable Player actor) {
        if (!usable()) return StartResult.NO_RECIPE;
        if (job != null) return StartResult.BUSY;
        if (!inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) return StartResult.OUTPUT_PENDING;
        if (ship == null || !permitted(ship)) return StartResult.INVALID_BLUEPRINT;
        ShipyardRecipe recipe = recipe(ship);
        if (recipe == null || !recipe.knownShip()) return StartResult.NO_RECIPE;
        ManufacturingMaterials.Plan plan = ManufacturingMaterials.plan(materials(), recipe);
        if (plan == null || tank.getFluidAmount() < recipe.fuel()
                || recipe.fuel() > 0 && !tank.getFluid().is(ModFluids.FUEL_OIL.get())) return StartResult.MISSING_RESOURCES;
        FluidStack paid = tank.drain(recipe.fuel(), IFluidHandler.FluidAction.SIMULATE);
        if (paid.getAmount() != recipe.fuel()) return StartResult.MISSING_RESOURCES;
        // 规划全部成功后再提交，找零空间不足不会回滚或阻止开工。
        job = new ShipyardJob(recipe, inventory.getStackInSlot(BLUEPRINT_SLOT), plan.ammunition(), paid);
        loggedJobState = null;
        for (int i = 0; i < ManufacturingMaterials.SLOTS; i++) inventory.setStackInSlot(CACHE_START + i, plan.remaining().get(i));
        tank.drain(recipe.fuel(), IFluidHandler.FluidAction.EXECUTE);
        for (ItemStack change : plan.change()) returnItem(change, actor);
        ModLog.machine(ModLog.Event.JOB_START, this, "job", job.id, "target", recipe.ship(),
                "fuel", recipe.fuel(), "ammunition", recipe.ammunition(), "steel", recipe.steel(),
                "aluminum", recipe.aluminum(), "seconds", recipe.seconds());
        changed(); return StartResult.STARTED;
    }

    public int destroyedFuelOnCancel() {
        return job == null ? 0 : job.fuel.getAmount() - tank.fill(job.fuel, IFluidHandler.FluidAction.SIMULATE);
    }

    public boolean cancel(UUID expected, @Nullable Player actor) {
        if (!usable() || job == null || !job.id.equals(expected)) return false;
        ShipyardJob cancelled = job; job = null;
        loggedJobState = null;
        for (ItemStack stack : ManufacturingMaterials.normalized("steel", cancelled.recipe.steel() * 9)) returnItem(stack, actor);
        for (ItemStack stack : ManufacturingMaterials.normalized("aluminum", cancelled.recipe.aluminum() * 9)) returnItem(stack, actor);
        for (ItemStack stack : cancelled.ammunition) returnItem(stack.copy(), actor);
        int returnedFuel = tank.fill(cancelled.fuel, IFluidHandler.FluidAction.EXECUTE);
        ModLog.machine(ModLog.Event.JOB_CANCEL, this, "job", cancelled.id, "target", cancelled.recipe.ship(),
                "elapsed", cancelled.elapsed(), "steel", cancelled.recipe.steel(), "aluminum", cancelled.recipe.aluminum(),
                "ammunitionItems", cancelled.ammunition.stream().mapToInt(ItemStack::getCount).sum(),
                "returnedFuel", returnedFuel, "discardedFuel", cancelled.fuel.getAmount() - returnedFuel);
        changed(); return true;
    }

    public void cancelCurrent(@Nullable Player actor) {
        if (job != null) cancel(job.id, actor);
    }

    // 创造拆除及无法采集的生存拆除按容器散落全部物品，不生成船坞物品或保留燃油。
    public void destroyCreative() {
        if (!usable()) return;
        cancelCurrent(null);
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            ItemStack contents = inventory.extractItem(slot, Integer.MAX_VALUE, false);
            if (!contents.isEmpty()) Block.popResource(level, worldPosition, contents);
        }
        tank.setFluid(FluidStack.EMPTY);
    }

    private void returnItem(ItemStack source, @Nullable Player actor) {
        ItemStack remainder = source.copy();
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = CACHE_START; slot < TOTAL_SLOTS && !remainder.isEmpty(); slot++) {
                boolean empty = inventory.getStackInSlot(slot).isEmpty();
                if ((pass == 0 && empty) || (pass == 1 && !empty)) continue;
                remainder = inventory.insertItem(slot, remainder, false);
            }
        }
        int outsideCache = remainder.getCount();
        if (actor != null && !remainder.isEmpty()) actor.getInventory().add(remainder);
        ModLog.machine(ModLog.Event.REFUND, this, "item", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.getItem()),
                "cache", source.getCount() - outsideCache, "inventory", outsideCache - remainder.getCount(), "dropped", remainder.getCount());
        if (!remainder.isEmpty() && level != null) {
            if (actor != null) actor.drop(remainder, false); else Block.popResource(level, worldPosition, remainder);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShipyardBlockEntity shipyard) {
        if (level.isClientSide || !shipyard.usable()) return;
        ShipyardJob job = shipyard.job;
        if (job == null) { shipyard.loggedJobState = null; return; }
        String status = !job.recipe.knownShip() ? "unknown_target" : !shipyard.permitted(job.recipe.ship())
                || !ItemStack.isSameItemSameComponents(shipyard.inventory.getStackInSlot(BLUEPRINT_SLOT), job.blueprint)
                ? "blueprint_mismatch" : job.finished() && !shipyard.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty() ? "output_pending" : "running";
        if (ModLog.debugEnabled() && !status.equals(shipyard.loggedJobState))
            ModLog.machine(ModLog.Event.JOB_STATE, shipyard, "job", job.id, "target", job.recipe.ship(), "state", status, "elapsed", job.elapsed());
        shipyard.loggedJobState = ModLog.debugEnabled() ? status : null;
        if (status.equals("unknown_target") || status.equals("blueprint_mismatch")) return;
        job.tick(); shipyard.changed();
        if (job.finished() && shipyard.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ItemStack output = RiggingCoreRules.create(job.recipe.ship());
            if (output.isEmpty()) return;
            shipyard.inventory.extractItem(BLUEPRINT_SLOT, 1, false);
            shipyard.inventory.setStackInSlot(OUTPUT_SLOT, output);
            ModLog.machine(ModLog.Event.JOB_FINISH, shipyard, "job", job.id, "target", job.recipe.ship(), "count", output.getCount());
            shipyard.job = null; shipyard.changed();
            level.playSound(null, pos, ModSounds.SHIPYARD_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public FluidActionResult transferContainer(ItemStack source, boolean inject, Player actor) {
        if (!usable() || source.isEmpty()) return FluidActionResult.FAILURE;
        int before = tank.getFluidAmount();
        FluidActionResult result = inject ? FluidUtil.tryEmptyContainer(source, fluidHandler, Integer.MAX_VALUE, actor, true)
                : FluidUtil.tryFillContainer(source, fluidHandler, Integer.MAX_VALUE, actor, true);
        if (result.isSuccess()) ModLog.machine(ModLog.Event.TRANSFER, this, "inject", inject, "amount", Math.abs(tank.getFluidAmount() - before));
        if (!result.isSuccess() || source.getCount() == 1) return result;
        ItemStack remaining = source.copy(); remaining.shrink(1);
        ItemStack converted = result.getResult().copy();
        if (!actor.getInventory().add(converted)) actor.drop(converted, false);
        return new FluidActionResult(remaining);
    }

    public boolean hasCachedContents() {
        return !tank.isEmpty() || java.util.stream.IntStream.range(0, TOTAL_SLOTS).anyMatch(i -> !inventory.getStackInSlot(i).isEmpty());
    }

    @Override public Component getDisplayName() { return Component.translatable("block.piranport.shipyard"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new ShipyardMenu(id, playerInventory, this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private void saveStorage(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("Fuel", tank.getFluid().saveOptional(registries)); tag.put("Browse", remembered.save());
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); saveStorage(tag, registries);
        if (job != null) tag.put("Construction", job.save(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        tank.setFluid(FluidStack.parseOptional(registries, tag.getCompound("Fuel")));
        remembered = ShipyardBrowseState.load(tag.getCompound("Browse"));
        job = tag.contains("Construction") ? ShipyardJob.load(tag.getCompound("Construction"), registries) : null;
        loggedJobState = null;
        invalidStoredJob = tag.contains("Construction") && (job == null || !job.recipe.knownShip());
        revision++;
    }

    @Override public void onLoad() {
        super.onLoad();
        if (invalidStoredJob) ModLog.invalidRestore(this, "Construction");
        else if (job != null) ModLog.machine(ModLog.Event.JOB_RESTORE, this, "job", job.id, "target", job.recipe.ship(), "elapsed", job.elapsed());
        invalidStoredJob = false;
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag data = new CompoundTag();
        if (level != null) { saveStorage(data, level.registryAccess()); components.set(ModDataComponents.SHIPYARD_STORAGE.get(), CustomData.of(data)); }
    }

    @Override protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(ModDataComponents.SHIPYARD_STORAGE.get());
        if (data != null && level != null) loadAdditional(data.copyTag(), level.registryAccess());
    }

    private final class FaceInventory implements IItemHandler {
        private final int[] slots;
        private final boolean input;
        private FaceInventory(int[] slots, boolean input) { this.slots = slots; this.input = input; }
        private int physical(int slot) { if (slot < 0 || slot >= slots.length) throw new IndexOutOfBoundsException(slot); return slots[slot]; }
        @Override public int getSlots() { return slots.length; }
        @Override public ItemStack getStackInSlot(int slot) { return usable() ? stack(physical(slot)) : ItemStack.EMPTY; }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return input ? insert(physical(slot), stack, simulate) : stack; }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            int index = physical(slot);
            if (index >= CACHE_START && !inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) return ItemStack.EMPTY;
            return extract(index, count, simulate);
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(physical(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return usable() && input && accepts(physical(slot), stack); }
    }

    private final class MenuInventory implements IItemHandlerModifiable {
        @Override public int getSlots() { return TOTAL_SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return level != null && level.isClientSide && accepts(slot, stack) ? inventory.insertItem(slot, stack, simulate) : insert(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            return level != null && level.isClientSide ? inventory.extractItem(slot, Math.min(count, extractable(slot)), simulate) : extract(slot, count, simulate);
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            if (level != null && level.isClientSide) { inventory.setStackInSlot(slot, stack); return; }
            if (!usable()) return;
            ItemStack old = inventory.getStackInSlot(slot);
            if (slot == BLUEPRINT_SLOT && job != null && (stack.getCount() < 1 || !ItemStack.isSameItemSameComponents(stack, job.blueprint))) return;
            if (!stack.isEmpty() && !accepts(slot, stack) && !(slot == OUTPUT_SLOT && ItemStack.isSameItemSameComponents(old, stack) && stack.getCount() <= old.getCount())) return;
            inventory.setStackInSlot(slot, stack);
        }
    }

    private final class FuelHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int index) { return usable() && index == 0 ? fuel() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int index) { return FUEL_CAPACITY; }
        @Override public boolean isFluidValid(int index, FluidStack stack) { return index == 0 && stack.is(ModFluids.FUEL_OIL.get()); }
        @Override public int fill(FluidStack stack, FluidAction action) { int amount = usable() ? tank.fill(stack, action) : 0;
            if (action.execute() && amount > 0) ModLog.aggregate(ModLog.Event.MACHINE_FILL, amount, true);
            return amount; }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { FluidStack drained = usable() ? tank.drain(stack, action) : FluidStack.EMPTY;
            if (action.execute() && !drained.isEmpty()) ModLog.aggregate(ModLog.Event.MACHINE_DRAIN, drained.getAmount(), true);
            return drained; }
        @Override public FluidStack drain(int amount, FluidAction action) { FluidStack drained = usable() ? tank.drain(amount, action) : FluidStack.EMPTY;
            if (action.execute() && !drained.isEmpty()) ModLog.aggregate(ModLog.Event.MACHINE_DRAIN, drained.getAmount(), true);
            return drained; }
    }
}
