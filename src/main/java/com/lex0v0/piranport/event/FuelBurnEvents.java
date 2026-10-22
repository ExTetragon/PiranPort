package com.lex0v0.piranport.event;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.registry.ModItems;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/**
 * 燃料值注册。
 */
@EventBusSubscriber(modid = PiranPort.MODID)
public final class FuelBurnEvents {
    private static final int CRUDE_OIL_BUCKET_BURN_TIME = 4800;
    private static final int FUEL_OIL_BUCKET_BURN_TIME = 12000;

    private FuelBurnEvents() {
    }

    @SubscribeEvent
    public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.CRUDE_OIL_BUCKET.get())) {
            event.setBurnTime(CRUDE_OIL_BUCKET_BURN_TIME);
        } else if (stack.is(ModItems.FUEL_OIL_BUCKET.get())) {
            event.setBurnTime(FUEL_OIL_BUCKET_BURN_TIME);
        }
    }
}
