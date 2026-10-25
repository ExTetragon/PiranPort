package com.lex0v0.piranport.client;

import com.lex0v0.piranport.logging.ModLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;

// 仅由已启用的自动验收调用，沿用原版回调完成首次启动引导。
final class AcceptanceClientStartup {
    private AcceptanceClientStartup() {}

    static boolean titleScreenReady(Minecraft minecraft) {
        if (minecraft.getOverlay() != null) return false;
        if (minecraft.screen instanceof AccessibilityOnboardingScreen onboarding) {
            onboarding.onClose();
            ModLog.onboardingDismissed();
            return false;
        }
        return minecraft.screen instanceof TitleScreen;
    }
}
