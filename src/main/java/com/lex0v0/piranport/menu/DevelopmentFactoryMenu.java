package com.lex0v0.piranport.menu;

import com.lex0v0.piranport.blockentity.DevelopmentFactoryBlockEntity;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryBrowseState;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryFilter;
import com.lex0v0.piranport.blockentity.manufacturing.ManufacturingMaterials;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryEntry;
import com.lex0v0.piranport.blockentity.developmentfactory.FactoryCatalog;
import com.lex0v0.piranport.item.BlueprintItem;
import com.lex0v0.piranport.network.DevelopmentFactorySyncPayload;
import com.lex0v0.piranport.registry.ModMenus;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

// 菜单保存窗口浏览状态，实际库存与开发事务仍由方块实体管理。
public final class DevelopmentFactoryMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = DevelopmentFactoryBlockEntity.TOTAL_SLOTS;
    public final DevelopmentFactoryBlockEntity factory;
    public FactoryBrowseState browse;
    private final Player viewer;
    private long sentRevision = Long.MIN_VALUE;

    public DevelopmentFactoryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, getClientEntity(inventory, buffer));
        CompoundTag state = buffer.readNbt();
        if (state != null) browse = FactoryBrowseState.load(state);
        normalizeBrowse();
    }

    private static DevelopmentFactoryBlockEntity getClientEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var entity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (!(entity instanceof DevelopmentFactoryBlockEntity factory)) throw new IllegalStateException("Missing factory at menu position");
        return factory;
    }

    public DevelopmentFactoryMenu(int id, Inventory inventory, DevelopmentFactoryBlockEntity factory) {
        super(ModMenus.DEVELOPMENT_FACTORY.get(), id);
        this.factory = factory; this.viewer = inventory.player; this.browse = factory.remembered();
        for (int slot = 0; slot < DevelopmentFactoryBlockEntity.CACHE_START; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(factory.menuInventory(), slot, slot == DevelopmentFactoryBlockEntity.BLUEPRINT_SLOT ? 249 : 359, 123) {
                @Override public boolean mayPickup(Player player) { return index == DevelopmentFactoryBlockEntity.BLUEPRINT_SLOT && factory.job() != null || super.mayPickup(player); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 4; col++)
            addSlot(new SlotItemHandler(factory.menuInventory(), DevelopmentFactoryBlockEntity.CACHE_START + row * 4 + col, 73 + col * 18, 165 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 169 + col * 18, 165 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 169 + col * 18, 223));
        normalizeBrowse();
    }

    public List<FactoryEntry> visibleEntries() {
        return visibleEntries(browse.filter);
    }

    public List<FactoryEntry> visibleEntries(FactoryFilter filter) {
        return FactoryCatalog.ALL.stream().filter(entry -> browse.all || factory.permitted(entry.id()))
                .filter(filter::matches).toList();
    }

    public void normalizeBrowse() {
        List<FactoryEntry> entries = visibleEntries();
        if (entries.stream().noneMatch(entry -> entry.id().equals(browse.selected))) browse.selected = null;
        browse.scroll = Math.clamp(browse.scroll, 0, Math.max(0, entries.size() - 4));
    }

    public void handleAction(int action, CompoundTag data) {
        if (!stillValid(viewer) || viewer.level().isClientSide) return;
        if (action == 0) { browse = FactoryBrowseState.load(data); normalizeBrowse(); }
        else if (viewer.isSpectator()) return;
        else if (action == 1) {
            ResourceLocation target = ResourceLocation.tryParse(data.getString("Target"));
            var result = factory.start(target, viewer);
            if (result != DevelopmentFactoryBlockEntity.StartResult.STARTED)
                viewer.displayClientMessage(Component.translatable("gui.piranport.development_factory.result." + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        } else if (action == 2 && data.hasUUID("Job")) factory.cancel(data.getUUID("Job"), viewer);
        else if (action == 3) {
            var result = factory.transferContainer(getCarried(), data.getBoolean("Inject"), viewer);
            if (result.isSuccess()) setCarried(result.getResult());
        }
        sentRevision = Long.MIN_VALUE;
        broadcastChanges();
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer instanceof ServerPlayer server && (sentRevision == Long.MIN_VALUE
                || (factory.revision() != sentRevision && viewer.tickCount % 5 == 0))) {
            PacketDistributor.sendToPlayer(server, new DevelopmentFactorySyncPayload(containerId, factory.saveWithoutMetadata(viewer.registryAccess())));
            sentRevision = factory.revision();
        }
    }

    public void receiveState(CompoundTag data) {
        if (viewer.level().isClientSide) { factory.loadWithComponents(data, viewer.registryAccess()); normalizeBrowse(); }
    }

    @Override public boolean stillValid(Player player) {
        return !factory.isRemoved() && player.level().getBlockEntity(factory.getBlockPos()) == factory
                && player.distanceToSqr(factory.getBlockPos().getCenter()) <= 64;
    }

    @Override public void removed(Player player) {
        if (!player.level().isClientSide) { normalizeBrowse(); factory.remember(browse); }
        super.removed(player);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (index == DevelopmentFactoryBlockEntity.BLUEPRINT_SLOT && factory.job() != null && type == ClickType.SWAP) return;
        super.clicked(index, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        ItemStack original = source.getItem().copy();
        if (original.isEmpty()) return ItemStack.EMPTY;
        if (index < MACHINE_SLOTS) {
            int count = Math.min(original.getCount(), factory.extractable(index));
            if (count == 0) return ItemStack.EMPTY;
            ItemStack moving = original.copyWithCount(count);
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            source.remove(count - moving.getCount()); source.setChanged();
        } else {
            ItemStack remaining = original.copy();
            if (original.getItem() instanceof BlueprintItem) {
                remaining = factory.menuInventory().insertItem(DevelopmentFactoryBlockEntity.BLUEPRINT_SLOT, remaining, false);
            } else if (ManufacturingMaterials.accepts(original)) {
                for (int i = DevelopmentFactoryBlockEntity.CACHE_START; i < MACHINE_SLOTS && !remaining.isEmpty(); i++) remaining = factory.menuInventory().insertItem(i, remaining, false);
            } else {
                if (index < MACHINE_SLOTS + 27) moveItemStackTo(remaining, MACHINE_SLOTS + 27, slots.size(), false);
                else moveItemStackTo(remaining, MACHINE_SLOTS, MACHINE_SLOTS + 27, false);
            }
            if (remaining.getCount() == original.getCount()) return ItemStack.EMPTY;
            source.set(remaining); source.setChanged();
        }
        source.onTake(player, original); return original;
    }
}
