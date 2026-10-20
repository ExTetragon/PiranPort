package com.lex0v0.piranport;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 公共配置。配置项按需添加，修改后可在 Mods 界面中调整。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("是否输出额外的调试日志")
            .define("debugLogging", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
