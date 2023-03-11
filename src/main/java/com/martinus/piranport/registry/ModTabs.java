package com.martinus.piranport.registry;

import com.martinus.piranport.PiranPort;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class ModTabs {
    public static final ItemGroup MODTAB_MAIN = new ItemGroup(PiranPort.MOD_ID + ".main") {
        public ItemStack makeIcon() {
            return new ItemStack(ModItems.MAIN_ICON.get());
        }
    };

    /*public static final ItemGroup MODTAB_ARM = new ItemGroup(piranport.MOD_ID + ".arm") {
        public ItemStack makeIcon() {
            return null;
        }
    };

    public static final ItemGroup MODTAB_FOOD = new ItemGroup(piranport.MOD_ID + ".food") {
        public ItemStack makeIcon() {
            return null;
        }
    };*/
}
