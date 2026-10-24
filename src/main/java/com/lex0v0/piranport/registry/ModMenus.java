package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.menu.ShipyardMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册带方块位置数据的船坞菜单。
public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, PiranPort.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ShipyardMenu>> SHIPYARD = MENUS.register("shipyard", () -> IMenuTypeExtension.create(ShipyardMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<com.lex0v0.piranport.menu.DevelopmentFactoryMenu>> DEVELOPMENT_FACTORY = MENUS.register("development_factory", () -> IMenuTypeExtension.create(com.lex0v0.piranport.menu.DevelopmentFactoryMenu::new));
    private ModMenus() {}
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
