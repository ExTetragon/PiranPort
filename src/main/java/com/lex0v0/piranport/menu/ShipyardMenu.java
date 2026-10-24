package com.lex0v0.piranport.menu;

import com.lex0v0.piranport.blockentity.ShipyardBlockEntity;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardBrowseState;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardFilter;
import com.lex0v0.piranport.blockentity.shipyard.ShipyardMaterials;
import com.lex0v0.piranport.domain.ship.ShipDefinition;
import com.lex0v0.piranport.domain.ship.ShipDefinitions;
import com.lex0v0.piranport.item.BlueprintItem;
import com.lex0v0.piranport.network.ShipyardSyncPayload;
import com.lex0v0.piranport.registry.ModMenus;
import java.util.List;
import java.util.Comparator;
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

// 菜单保存窗口浏览状态，实际库存与建造事务仍由方块实体管理。
public final class ShipyardMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = ShipyardBlockEntity.TOTAL_SLOTS;
    public final ShipyardBlockEntity shipyard;
    public ShipyardBrowseState browse;
    private final Player viewer;
    private long sentRevision = Long.MIN_VALUE;

    public ShipyardMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, getClientEntity(inventory, buffer));
        CompoundTag state = buffer.readNbt();
        if (state != null) browse = ShipyardBrowseState.load(state);
        normalizeBrowse();
    }

    private static ShipyardBlockEntity getClientEntity(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        var entity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (!(entity instanceof ShipyardBlockEntity shipyard)) throw new IllegalStateException("Missing shipyard at menu position");
        return shipyard;
    }

    public ShipyardMenu(int id, Inventory inventory, ShipyardBlockEntity shipyard) {
        super(ModMenus.SHIPYARD.get(), id);
        this.shipyard = shipyard; this.viewer = inventory.player; this.browse = shipyard.remembered();
        for (int slot = 0; slot < ShipyardBlockEntity.CACHE_START; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(shipyard.menuInventory(), slot, slot == ShipyardBlockEntity.BLUEPRINT_SLOT ? 213 : 323, 123) {
                @Override public boolean mayPickup(Player player) { return index == ShipyardBlockEntity.BLUEPRINT_SLOT && shipyard.job() != null || super.mayPickup(player); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 4; col++)
            addSlot(new SlotItemHandler(shipyard.menuInventory(), ShipyardBlockEntity.CACHE_START + row * 4 + col, 55 + col * 18, 165 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 151 + col * 18, 165 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 151 + col * 18, 223));
        normalizeBrowse();
    }

    public List<ShipDefinition> visibleShips() {
        return visibleShips(browse.filter);
    }

    public List<ShipDefinition> visibleShips(ShipyardFilter filter) {
        return ShipDefinitions.ALL.stream().filter(ship -> browse.all || shipyard.permitted(ship.id()))
                .filter(filter::matches)
                .sorted(Comparator.comparingInt((ShipDefinition ship) -> ship.size().ordinal())
                        .thenComparingInt(ship -> ship.type().ordinal())).toList();
    }

    public void normalizeBrowse() {
        List<ShipDefinition> ships = visibleShips();
        if (ships.stream().noneMatch(ship -> ship.id().equals(browse.selected))) browse.selected = null;
        browse.scroll = Math.clamp(browse.scroll, 0, Math.max(0, ships.size() - 4));
    }

    public void handleAction(int action, CompoundTag data) {
        if (!stillValid(viewer) || viewer.level().isClientSide) return;
        if (action == 0) { browse = ShipyardBrowseState.load(data); normalizeBrowse(); }
        else if (viewer.isSpectator()) return;
        else if (action == 1) {
            ResourceLocation target = ResourceLocation.tryParse(data.getString("Ship"));
            var result = shipyard.start(target, viewer);
            if (result != ShipyardBlockEntity.StartResult.STARTED)
                viewer.displayClientMessage(Component.translatable("gui.piranport.shipyard.result." + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        } else if (action == 2 && data.hasUUID("Job")) shipyard.cancel(data.getUUID("Job"), viewer);
        else if (action == 3) {
            var result = shipyard.transferContainer(getCarried(), data.getBoolean("Inject"), viewer);
            if (result.isSuccess()) setCarried(result.getResult());
        }
        sentRevision = Long.MIN_VALUE;
        broadcastChanges();
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer instanceof ServerPlayer server && (sentRevision == Long.MIN_VALUE
                || (shipyard.revision() != sentRevision && viewer.tickCount % 5 == 0))) {
            PacketDistributor.sendToPlayer(server, new ShipyardSyncPayload(containerId, shipyard.saveWithoutMetadata(viewer.registryAccess())));
            sentRevision = shipyard.revision();
        }
    }

    public void receiveState(CompoundTag data) {
        if (viewer.level().isClientSide) { shipyard.loadWithComponents(data, viewer.registryAccess()); normalizeBrowse(); }
    }

    @Override public boolean stillValid(Player player) {
        return !shipyard.isRemoved() && player.level().getBlockEntity(shipyard.getBlockPos()) == shipyard
                && player.distanceToSqr(shipyard.getBlockPos().getCenter()) <= 64;
    }

    @Override public void removed(Player player) {
        if (!player.level().isClientSide) { normalizeBrowse(); shipyard.remember(browse); }
        super.removed(player);
    }

    @Override public void clicked(int index, int button, ClickType type, Player player) {
        if (index == ShipyardBlockEntity.BLUEPRINT_SLOT && shipyard.job() != null && type == ClickType.SWAP) return;
        super.clicked(index, button, type, player);
    }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.isActive() && super.canTakeItemForPickAll(stack, slot); }
    @Override public boolean canDragTo(Slot slot) { return slot.isActive() && super.canDragTo(slot); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).isActive()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        ItemStack original = source.getItem().copy();
        if (original.isEmpty()) return ItemStack.EMPTY;
        if (index < MACHINE_SLOTS) {
            int count = Math.min(original.getCount(), shipyard.extractable(index));
            if (count == 0) return ItemStack.EMPTY;
            ItemStack moving = original.copyWithCount(count);
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            source.remove(count - moving.getCount()); source.setChanged();
        } else {
            ItemStack remaining = original.copy();
            if (original.getItem() instanceof BlueprintItem) {
                remaining = shipyard.menuInventory().insertItem(ShipyardBlockEntity.BLUEPRINT_SLOT, remaining, false);
            } else if (ShipyardMaterials.accepts(original)) {
                for (int i = ShipyardBlockEntity.CACHE_START; i < MACHINE_SLOTS && !remaining.isEmpty(); i++) remaining = shipyard.menuInventory().insertItem(i, remaining, false);
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
