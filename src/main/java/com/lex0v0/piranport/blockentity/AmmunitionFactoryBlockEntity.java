package com.lex0v0.piranport.blockentity;

import com.lex0v0.piranport.blockentity.ammunitionfactory.AmmunitionBrowseState;
import com.lex0v0.piranport.blockentity.ammunitionfactory.AmmunitionJob;
import com.lex0v0.piranport.blockentity.manufacturing.AmmunitionMaterials;
import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.menu.AmmunitionFactoryMenu;
import com.lex0v0.piranport.recipe.AmmunitionFactoryRecipe;
import com.lex0v0.piranport.registry.*;
import java.util.ArrayList;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// 一台机器持有一份有限计划，每次投入、加工和整批入库单独结算。
public final class AmmunitionFactoryBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MATERIAL_SLOTS=8, OUTPUT_START=8, TOTAL_SLOTS=16;
    private AmmunitionJob job;
    private AmmunitionBrowseState remembered=new AmmunitionBrowseState();
    private long revision;
    private boolean harvestDrop, removalSettled, invalidJob;
    private String loggedState;
    private final ItemStackHandler inventory=new ItemStackHandler(TOTAL_SLOTS){
        @Override protected void onContentsChanged(int slot){changed();}
        @Override public boolean isItemValid(int slot,ItemStack stack){return accepts(slot,stack);}
    };
    private final IItemHandlerModifiable menuInventory=new MenuInventory();
    private final IItemHandler sides=new FaceInventory(true), bottom=new FaceInventory(false);

    public AmmunitionFactoryBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.AMMUNITION_FACTORY.get(),pos,state);}
    private boolean usable(){return level!=null&&!level.isClientSide&&!isRemoved()&&level.getBlockEntity(worldPosition)==this;}
    private void changed(){revision++;setChanged();}
    public long revision(){return revision;}
    public @Nullable AmmunitionJob job(){return job;}
    public ItemStack stack(int slot){return inventory.getStackInSlot(slot).copy();}
    public IItemHandlerModifiable menuInventory(){return menuInventory;}
    public @Nullable IItemHandler items(@Nullable Direction side){return side==Direction.UP?null:side==Direction.DOWN?bottom:sides;}
    public AmmunitionBrowseState remembered(){return remembered.copy();}
    public void remember(AmmunitionBrowseState state){if(usable()){remembered=state.copy();changed();}}
    public List<ItemStack> materials(){return java.util.stream.IntStream.range(0,MATERIAL_SLOTS).mapToObj(this::stack).toList();}
    public boolean accepts(int slot,ItemStack stack){return slot>=0&&slot<MATERIAL_SLOTS&&AmmunitionMaterials.accepts(stack);}
    public ItemStack insert(int slot,ItemStack stack,boolean simulate){return usable()&&accepts(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
    public ItemStack extract(int slot,int count,boolean simulate){return usable()&&slot>=0&&slot<TOTAL_SLOTS?inventory.extractItem(slot,Math.max(0,count),simulate):ItemStack.EMPTY;}
    public boolean hasProducts(){for(int i=OUTPUT_START;i<TOTAL_SLOTS;i++)if(!inventory.getStackInSlot(i).isEmpty())return true;return false;}
    public @Nullable RecipeHolder<AmmunitionFactoryRecipe> recipeHolder(ResourceLocation target){
        if(level==null||target==null)return null;
        var recipes=level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AMMUNITION_FACTORY.get()).stream().filter(h->h.value().target().equals(target)).toList();
        return recipes.size()==1?recipes.getFirst():null;
    }
    public @Nullable AmmunitionFactoryRecipe recipe(ResourceLocation target){var holder=recipeHolder(target);return holder==null?null:holder.value();}
    public int maximum(AmmunitionFactoryRecipe recipe){return AmmunitionMaterials.maximum(materials(),recipe);}

    // 在副本中检查全部堆叠与组件，失败时一枚产物也不写入。
    private @Nullable List<ItemStack> outputPlan(ItemStack result){
        if(result.isEmpty())return null;
        List<ItemStack> output=new ArrayList<>();for(int i=OUTPUT_START;i<TOTAL_SLOTS;i++)output.add(stack(i));
        int remaining=result.getCount();
        for(int pass=0;pass<2;pass++)for(int i=0;i<output.size()&&remaining>0;i++){
            ItemStack present=output.get(i);if(pass==0&&present.isEmpty()||pass==1&&!present.isEmpty())continue;
            if(!present.isEmpty()&&!ItemStack.isSameItemSameComponents(result,present))continue;
            int room=Math.max(0,Math.min(64,result.getMaxStackSize())-present.getCount());int moved=Math.min(room,remaining);
            if(moved>0){output.set(i,result.copyWithCount(present.getCount()+moved));remaining-=moved;}
        }
        return remaining==0?output:null;
    }
    public boolean canFit(AmmunitionFactoryRecipe recipe){return recipe!=null&&outputPlan(recipe.result())!=null;}
    public enum StartResult {STARTED,BUSY,NO_RECIPE,INVALID_QUANTITY,MISSING_RESOURCES,OUTPUT_FULL}
    public StartResult start(ResourceLocation target,int requested){
        if(!usable())return StartResult.NO_RECIPE;
        if(job!=null)return StartResult.BUSY;
        var holder=recipeHolder(target);if(holder==null||!holder.value().valid())return StartResult.NO_RECIPE;
        if(requested<=0)return StartResult.INVALID_QUANTITY;
        var recipe=holder.value();int count=AmmunitionMaterials.normalize(requested,recipe.count(),maximum(recipe));
        if(count==0)return StartResult.MISSING_RESOURCES;
        if(!canFit(recipe))return StartResult.OUTPUT_FULL;
        job=new AmmunitionJob(holder.id(),recipe,count/recipe.count());
        if(!beginBatch()){job=null;return StartResult.MISSING_RESOURCES;}
        ModLog.machine(ModLog.Event.JOB_START,this,"job",job.id,"target",target,"pieces",count,"runs",job.totalRuns);
        changed();return StartResult.STARTED;
    }
    private boolean beginBatch(){
        if(job==null||job.inFlight||job.stopping||job.completedRuns>=job.totalRuns||!canFit(job.recipe))return false;
        var plan=AmmunitionMaterials.plan(materials(),job.recipe);if(plan==null)return false;
        job.inFlight=true;job.elapsed=0;
        for(int i=0;i<MATERIAL_SLOTS;i++)inventory.setStackInSlot(i,plan.remaining().get(i));
        for(ItemStack change:plan.change())returnChange(change);
        changed();return true;
    }
    private void returnChange(ItemStack source){
        ItemStack remaining=source.copy();
        for(int pass=0;pass<2;pass++)for(int i=0;i<MATERIAL_SLOTS&&!remaining.isEmpty();i++){
            boolean empty=inventory.getStackInSlot(i).isEmpty();if(pass==0&&empty||pass==1&&!empty)continue;
            remaining=inventory.insertItem(i,remaining,false);
        }
        for(Direction side:Direction.Plane.HORIZONTAL){
            if(remaining.isEmpty())break;
            BlockPos other=worldPosition.relative(side);if(!level.hasChunkAt(other))continue;
            var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,other,side.getOpposite());
            if(handler==null||handler==sides||handler==bottom)continue;
            for(int i=0;i<handler.getSlots()&&!remaining.isEmpty();i++)remaining=handler.insertItem(i,remaining,false);
        }
        if(!remaining.isEmpty())Block.popResource(level,worldPosition,remaining);
    }
    public boolean interrupt(UUID expected){
        if(!usable()||job==null||!job.id.equals(expected))return false;
        ModLog.machine(ModLog.Event.ACTION,this,"action","interrupt","job",expected);
        if(job.inFlight)job.stopping=true;else job=null;
        changed();return true;
    }
    public String status(){
        if(job==null)return "idle";
        if(job.inFlight)return job.finishedBatch()&&!canFit(job.recipe)?"pending":job.stopping?"stopping":"running";
        return AmmunitionMaterials.plan(materials(),job.recipe)==null?"materials":"output";
    }
    public static void tick(Level level,BlockPos pos,BlockState state,AmmunitionFactoryBlockEntity factory){
        if(!factory.usable()||factory.job==null)return;
        var job=factory.job;
        String status=factory.status();
        if(!status.equals(factory.loggedState))ModLog.machine(ModLog.Event.JOB_STATE,factory,"job",job.id,"state",status);
        factory.loggedState=status;
        if(!job.inFlight){factory.beginBatch();return;}
        if(job.elapsed<job.recipe.ticks()){job.elapsed++;factory.changed();}
        if(!job.finishedBatch())return;
        var output=factory.outputPlan(job.recipe.result());if(output==null)return;
        for(int i=0;i<output.size();i++)factory.inventory.setStackInSlot(OUTPUT_START+i,output.get(i));
        job.completedRuns++;job.inFlight=false;job.elapsed=0;factory.changed();
        ModLog.aggregate(ModLog.Event.AMMUNITION_BATCH,job.recipe.count(),true);
        if(job.stopping||job.completedRuns==job.totalRuns){
            ModLog.machine(ModLog.Event.JOB_FINISH,factory,"job",job.id,"target",job.recipe.target(),"pieces",job.completedPieces(),"interrupted",job.stopping);
            factory.job=null;factory.loggedState=null;factory.changed();
            level.playSound(null,pos,ModSounds.SHIPYARD_COMPLETE.get(),SoundSource.BLOCKS,1,1);
        }else factory.beginBatch();
    }

    // 正确工具仅让随后的正常战利品携带缓存，其他移除直接散落且只结算一次。
    public void prepareHarvest(boolean matching){
        if(!usable())return;
        harvestDrop=matching;job=null;changed();
        if(!matching)discardForRemoval();
    }
    public boolean harvestDrop(){return harvestDrop;}
    public void discardForRemoval(){
        if(removalSettled||level==null||level.isClientSide)return;
        removalSettled=true;job=null;
        if(!harvestDrop)for(int i=0;i<TOTAL_SLOTS;i++){
            ItemStack stack=inventory.getStackInSlot(i).copy();inventory.setStackInSlot(i,ItemStack.EMPTY);
            if(!stack.isEmpty())Block.popResource(level,worldPosition,stack);
        }
        changed();
    }
    @Override public Component getDisplayName(){return Component.translatable("block.piranport.ammunition_factory");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new AmmunitionFactoryMenu(id,inv,this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider registries){
        super.saveAdditional(t,registries);t.put("Inventory",inventory.serializeNBT(registries));t.put("Browse",remembered.save());if(job!=null)t.put("Job",job.save());
    }
    private void loadInventory(CompoundTag t,HolderLookup.Provider registries){
        if(t.getInt("Size")==TOTAL_SLOTS)inventory.deserializeNBT(registries,t);
        else ModLog.invalidRestore(this,"Inventory");
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider registries){
        super.loadAdditional(t,registries);loadInventory(t.getCompound("Inventory"),registries);remembered=AmmunitionBrowseState.load(t.getCompound("Browse"));
        job=t.contains("Job")?AmmunitionJob.load(t.getCompound("Job")):null;invalidJob=t.contains("Job")&&job==null;loggedState=null;revision++;
    }
    @Override public void onLoad(){super.onLoad();if(invalidJob)ModLog.invalidRestore(this,"Job");else if(job!=null)ModLog.machine(ModLog.Event.JOB_RESTORE,this,"job",job.id,"completed",job.completedRuns);invalidJob=false;}
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components){
        super.collectImplicitComponents(components);
        if(level!=null){CompoundTag t=new CompoundTag();t.put("Inventory",inventory.serializeNBT(level.registryAccess()));components.set(ModDataComponents.AMMUNITION_FACTORY_STORAGE.get(),CustomData.of(t));}
    }
    @Override protected void applyImplicitComponents(DataComponentInput input){
        super.applyImplicitComponents(input);CustomData t=input.get(ModDataComponents.AMMUNITION_FACTORY_STORAGE.get());
        if(t!=null&&level!=null){loadInventory(t.copyTag().getCompound("Inventory"),level.registryAccess());job=null;remembered=new AmmunitionBrowseState();}
    }
    private final class FaceInventory implements IItemHandler {
        private final boolean input;
        FaceInventory(boolean input){this.input=input;}
        private int physical(int slot){if(slot<0||slot>=getSlots())throw new IndexOutOfBoundsException(slot);return slot<MATERIAL_SLOTS?slot+OUTPUT_START:slot-OUTPUT_START;}
        @Override public int getSlots(){return TOTAL_SLOTS;}
        @Override public ItemStack getStackInSlot(int slot){return usable()?stack(physical(slot)):ItemStack.EMPTY;}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return input?insert(physical(slot),stack,simulate):stack;}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){int index=physical(slot);return index<OUTPUT_START&&hasProducts()?ItemStack.EMPTY:extract(index,amount,simulate);}
        @Override public int getSlotLimit(int slot){physical(slot);return 64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return usable()&&input&&accepts(physical(slot),stack);}
    }
    private final class MenuInventory implements IItemHandlerModifiable {
        @Override public int getSlots(){return TOTAL_SLOTS;}
        @Override public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
        @Override public int getSlotLimit(int slot){return 64;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return accepts(slot,stack);}
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return level!=null&&level.isClientSide&&accepts(slot,stack)?inventory.insertItem(slot,stack,simulate):insert(slot,stack,simulate);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return level!=null&&level.isClientSide?inventory.extractItem(slot,Math.max(0,amount),simulate):extract(slot,amount,simulate);}
        @Override public void setStackInSlot(int slot,ItemStack stack){
            if(level!=null&&level.isClientSide){inventory.setStackInSlot(slot,stack);return;}
            if(!usable())return;
            ItemStack old=inventory.getStackInSlot(slot);
            boolean shrinking=stack.isEmpty()||ItemStack.isSameItemSameComponents(old,stack)&&stack.getCount()<=old.getCount();
            if(shrinking||accepts(slot,stack))inventory.setStackInSlot(slot,stack.copy());
        }
    }
}
