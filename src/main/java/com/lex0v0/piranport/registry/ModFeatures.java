package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.worldgen.OilLakeConfiguration;
import com.lex0v0.piranport.worldgen.OilLakeFeature;
import com.lex0v0.piranport.worldgen.SaltpeterPatchConfiguration;
import com.lex0v0.piranport.worldgen.SaltpeterPatchFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// 注册硝土簇和油湖的生成特征。
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, PiranPort.MODID);

    public static final DeferredHolder<Feature<?>, SaltpeterPatchFeature> SALTPETER_PATCH =
            FEATURES.register("saltpeter_patch",
                    () -> new SaltpeterPatchFeature(SaltpeterPatchConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, OilLakeFeature> OIL_LAKE =
            FEATURES.register("oil_lake", () -> new OilLakeFeature(OilLakeConfiguration.CODEC));

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
