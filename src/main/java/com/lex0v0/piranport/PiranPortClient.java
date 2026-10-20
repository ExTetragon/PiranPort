package com.lex0v0.piranport;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专用入口。该类不会在专用服务器上加载，可以安全访问客户端代码。
 */
@Mod(value = PiranPort.MODID, dist = Dist.CLIENT)
public class PiranPortClient {
    public PiranPortClient(ModContainer container) {
        // 在 Mods 界面中为配置项生成图形界面。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
