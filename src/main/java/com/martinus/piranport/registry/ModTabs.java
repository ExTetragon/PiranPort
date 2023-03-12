package com.martinus.piranport.registry;

import com.martinus.piranport.PiranPort;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final CreativeModeTab MODTAB_MAIN = new CreativeModeTab(PiranPort.MOD_ID + ".main") {
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.MAIN_ICON.get());
        }
    };

    /*public static final CreativeModeTab MODTAB_ARM = new CreativeModeTab(WarshipGirlsR.MOD_ID + ".arm") {
        public ItemStack makeIcon() {
            return null;
        }
    };*/

    public static final CreativeModeTab MODTAB_FOOD = new CreativeModeTab(PiranPort.MOD_ID + ".food") {
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.MAIN_ICON.get());
        }
    };
}
