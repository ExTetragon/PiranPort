package com.lex0v0.piranport.menu;

import com.lex0v0.piranport.blockentity.AmmunitionFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.ammunitionfactory.AmmunitionBrowseState;
import com.lex0v0.piranport.blockentity.manufacturing.AmmunitionMaterials;
import com.lex0v0.piranport.logging.ModLog;
import com.lex0v0.piranport.network.payload.AmmunitionFactorySyncPayload;
import com.lex0v0.piranport.registry.ModMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

// 浏览草稿归菜单，实际库存与有限计划归机器。
public final class AmmunitionFactoryMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS=16;
    // 绘制与点击共用坐标；提示下方四段留白均为4像素。
    public static final int INPUT_Y=44,ARROW_Y=84,OUTPUT_Y=108;
    public final AmmunitionFactoryBlockEntity factory;
    public AmmunitionBrowseState browse;
    private final Player viewer;
    private long sentRevision=Long.MIN_VALUE;
    public AmmunitionFactoryMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){
        this(id,inventory,clientEntity(inventory,buffer));var t=buffer.readNbt();if(t!=null)browse=AmmunitionBrowseState.load(t);normalizeQuantity();
    }
    private static AmmunitionFactoryBlockEntity clientEntity(Inventory inventory,RegistryFriendlyByteBuf buffer){
        if(inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof AmmunitionFactoryBlockEntity f)return f;
        throw new IllegalStateException("Missing ammunition factory");
    }
    public AmmunitionFactoryMenu(int id,Inventory inventory,AmmunitionFactoryBlockEntity factory){
        super(ModMenus.AMMUNITION_FACTORY.get(),id);this.factory=factory;viewer=inventory.player;browse=factory.remembered();
        for(int row=0;row<2;row++)for(int col=0;col<4;col++)addSlot(new SlotItemHandler(factory.menuInventory(),row*4+col,261+col*18,INPUT_Y+1+row*18));
        for(int row=0;row<2;row++)for(int col=0;col<4;col++)addSlot(new SlotItemHandler(factory.menuInventory(),8+row*4+col,261+col*18,OUTPUT_Y+1+row*18));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,94+col*18,171+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,94+col*18,229));
        normalizeQuantity();
    }
    public void normalizeQuantity(){var r=factory.recipe(browse.selected.id());browse.quantity=r==null?0:AmmunitionMaterials.normalize(browse.quantity,r.count(),factory.maximum(r));}
    public void handleAction(int action,CompoundTag data){
        if(viewer.level().isClientSide||!stillValid(viewer))return;
        if(action==0){browse=AmmunitionBrowseState.load(data);if(data.getBoolean("CommitQuantity"))normalizeQuantity();}
        else if(viewer.isSpectator())return;
        else if(action==1){
            var target=ResourceLocation.tryParse(data.getString("Target"));
            var result=factory.start(target,data.getInt("Quantity"));
            if(result==AmmunitionFactoryBlockEntity.StartResult.STARTED)browse.quantity=factory.job().totalPieces();
            else viewer.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.piranport.ammunition_factory.result."+result.name().toLowerCase(java.util.Locale.ROOT)),true);
            ModLog.machine(ModLog.Event.ACTION,factory,"action","start","target",target,"result",result);
        }else if(action==2&&data.hasUUID("Job"))factory.interrupt(data.getUUID("Job"));
        sentRevision=Long.MIN_VALUE;broadcastChanges();
    }
    @Override public void broadcastChanges(){
        super.broadcastChanges();
        if(viewer instanceof ServerPlayer p&&(sentRevision==Long.MIN_VALUE||factory.revision()!=sentRevision&&viewer.tickCount%5==0)){
            PacketDistributor.sendToPlayer(p,new AmmunitionFactorySyncPayload(containerId,factory.saveWithoutMetadata(viewer.registryAccess()),browse.save()));sentRevision=factory.revision();
        }
    }
    public void receiveState(CompoundTag state,CompoundTag browser){
        if(viewer.level().isClientSide){factory.loadWithComponents(state,viewer.registryAccess());browse=AmmunitionBrowseState.load(browser);}
    }
    @Override public boolean stillValid(Player p){return !factory.isRemoved()&&p.level().getBlockEntity(factory.getBlockPos())==factory&&p.distanceToSqr(factory.getBlockPos().getCenter())<=64;}
    @Override public void removed(Player p){if(!p.level().isClientSide)factory.remember(browse);super.removed(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot source=slots.get(index);ItemStack original=source.getItem().copy();if(original.isEmpty())return ItemStack.EMPTY;
        if(index<MACHINE_SLOTS){
            ItemStack moving=original.copy();if(!moveItemStackTo(moving,MACHINE_SLOTS,slots.size(),true))return ItemStack.EMPTY;
            source.remove(original.getCount()-moving.getCount());source.setChanged();
        }else{
            ItemStack remaining=original.copy();
            if(AmmunitionMaterials.accepts(remaining)){
                for(int pass=0;pass<2;pass++)for(int i=0;i<8&&!remaining.isEmpty();i++){
                    boolean empty=factory.stack(i).isEmpty();if(pass==0&&empty||pass==1&&!empty)continue;
                    remaining=factory.insert(i,remaining,false);
                }
            }else if(index<MACHINE_SLOTS+27)moveItemStackTo(remaining,MACHINE_SLOTS+27,slots.size(),false);
            else moveItemStackTo(remaining,MACHINE_SLOTS,MACHINE_SLOTS+27,false);
            if(remaining.getCount()==original.getCount())return ItemStack.EMPTY;
            source.set(remaining);source.setChanged();
        }
        source.onTake(p,original);return original;
    }
}
