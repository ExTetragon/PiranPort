package com.lex0v0.piranport.client;

import com.lex0v0.piranport.PiranPort;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// 仅在客户端挂接配置界面，保持专用服务器加载边界。
@Mod(value = PiranPort.MODID, dist = Dist.CLIENT)
public class PiranPortClient {
    public PiranPortClient(ModContainer container) {
        // 在 Mods 界面中为配置项生成图形界面。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
