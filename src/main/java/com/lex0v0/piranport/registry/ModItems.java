package com.lex0v0.piranport.registry;

import com.lex0v0.piranport.PiranPort;
import com.lex0v0.piranport.item.FuelBarrelItem;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册入口。
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PiranPort.MODID);

    // 舰R食物
    public static final DeferredItem<Item> TAPTAP_ICE_CREAM =
            registerFood("taptap_ice_cream", FoodValues.TAPTAP_ICE_CREAM);

    public static final DeferredItem<Item> INSTANT_NOODLES =
            registerFood("instant_noodles", FoodValues.INSTANT_NOODLES);

    public static final DeferredItem<Item> TOAST_BREAD =
            registerFood("toast_bread", FoodValues.TOAST_BREAD);

    public static final DeferredItem<Item> NAVY_BAKED_BEANS =
            registerFood("navy_baked_beans", FoodValues.NAVY_BAKED_BEANS);

    public static final DeferredItem<Item> SURSTROMMING =
            registerFood("surstromming", FoodValues.SURSTROMMING);

    public static final DeferredItem<Item> LATIAO =
            registerFood("latiao", FoodValues.LATIAO);

    public static final DeferredItem<Item> ASSORTED_CHAR_SIU_FRIED_RICE =
            registerFood("assorted_char_siu_fried_rice", FoodValues.ASSORTED_CHAR_SIU_FRIED_RICE);

    public static final DeferredItem<Item> DELUXE_BAOZI =
            registerFood("deluxe_baozi", FoodValues.DELUXE_BAOZI);

    public static final DeferredItem<Item> SWEET_TOFU_PUDDING =
            registerFood("sweet_tofu_pudding", FoodValues.SWEET_TOFU_PUDDING);

    public static final DeferredItem<Item> SAVORY_TOFU_PUDDING =
            registerFood("savory_tofu_pudding", FoodValues.SAVORY_TOFU_PUDDING);

    public static final DeferredItem<Item> DONGPO_PORK =
            registerFood("dongpo_pork", FoodValues.DONGPO_PORK);

    public static final DeferredItem<Item> MAPO_TOFU =
            registerFood("mapo_tofu", FoodValues.MAPO_TOFU);

    public static final DeferredItem<Item> TANGYUAN =
            registerFood("tangyuan", FoodValues.TANGYUAN);

    public static final DeferredItem<Item> BOUILLABAISSE =
            registerFood("bouillabaisse", FoodValues.BOUILLABAISSE);

    public static final DeferredItem<Item> TARTE_TATIN =
            registerFood("tarte_tatin", FoodValues.TARTE_TATIN);

    public static final DeferredItem<Item> MACARON =
            registerFood("macaron", FoodValues.MACARON);

    public static final DeferredItem<Item> RYE_BREAD =
            registerFood("rye_bread", FoodValues.RYE_BREAD);

    public static final DeferredItem<Item> FRESH_RYE_BREAD =
            registerFood("fresh_rye_bread", FoodValues.FRESH_RYE_BREAD);

    public static final DeferredItem<Item> PELMENI =
            registerFood("pelmeni", FoodValues.PELMENI);

    public static final DeferredItem<Item> KHOLODETS =
            registerFood("kholodets", FoodValues.KHOLODETS);

    public static final DeferredItem<Item> BORSCHT =
            registerFood("borscht", FoodValues.BORSCHT);

    public static final DeferredItem<Item> KVASS =
            registerFood("kvass", FoodValues.KVASS);

    public static final DeferredItem<Item> PLATED_ROYAL_NAVAL_SALTED_BEEF =
            registerFood("plated_royal_naval_salted_beef", FoodValues.PLATED_ROYAL_NAVAL_SALTED_BEEF);

    public static final DeferredItem<Item> ROYAL_NAVAL_SALTED_BEEF =
            ITEMS.registerSimpleItem("royal_naval_salted_beef", new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> BLACK_TEA_SANDWICH =
            registerFood("black_tea_sandwich", FoodValues.BLACK_TEA_SANDWICH);

    public static final DeferredItem<Item> BLACK_TEA_SCONE =
            registerFood("black_tea_scone", FoodValues.BLACK_TEA_SCONE);

    public static final DeferredItem<Item> FISH_AND_CHIPS =
            registerFood("fish_and_chips", FoodValues.FISH_AND_CHIPS);

    public static final DeferredItem<Item> STARGAZY_PIE =
            registerFood("stargazy_pie", FoodValues.STARGAZY_PIE);

    public static final DeferredItem<Item> AMERICAN_BURGER =
            registerFood("american_burger", FoodValues.AMERICAN_BURGER);

    public static final DeferredItem<Item> DOUBLE_AMERICAN_BURGER =
            registerFood("double_american_burger", FoodValues.DOUBLE_AMERICAN_BURGER);

    public static final DeferredItem<Item> TRIPLE_AMERICAN_BURGER =
            registerFood("triple_american_burger", FoodValues.TRIPLE_AMERICAN_BURGER);

    public static final DeferredItem<Item> EGGS_BENEDICT =
            registerFood("eggs_benedict", FoodValues.EGGS_BENEDICT);

    public static final DeferredItem<Item> HOT_DOG =
            registerFood("hot_dog", FoodValues.HOT_DOG);

    public static final DeferredItem<Item> BARBECUE =
            registerFood("barbecue", FoodValues.BARBECUE);

    public static final DeferredItem<Item> APPLE_PIE =
            registerFood("apple_pie", FoodValues.APPLE_PIE);

    public static final DeferredItem<Item> TORPEDO_JUICE =
            registerFood("torpedo_juice", FoodValues.TORPEDO_JUICE);

    public static final DeferredItem<Item> SALAMI_PIZZA =
            registerFood("salami_pizza", FoodValues.SALAMI_PIZZA);

    public static final DeferredItem<Item> SALAMI_PIZZA_SLICES =
            registerFood("salami_pizza_slices", FoodValues.SALAMI_PIZZA_SLICES);

    public static final DeferredItem<Item> BOLOGNESE_LINGUINE =
            registerFood("bolognese_linguine", FoodValues.BOLOGNESE_LINGUINE);

    public static final DeferredItem<Item> MUSSOLINIS_OO =
            registerFood("mussolinis_oo", FoodValues.MUSSOLINIS_OO);

    public static final DeferredItem<Item> THURINGIAN_ROSTBRATWURST_WITH_BEER =
            registerFood("thuringian_rostbratwurst_with_beer", FoodValues.THURINGIAN_ROSTBRATWURST_WITH_BEER);

    public static final DeferredItem<Item> THURINGIAN_ROSTBRATWURST =
            registerFood("thuringian_rostbratwurst", FoodValues.THURINGIAN_ROSTBRATWURST);

    public static final DeferredItem<Item> SCHWEINSHAXE =
            registerFood("schweinshaxe", FoodValues.SCHWEINSHAXE);

    public static final DeferredItem<Item> LABLAB_BEAN_SOUP_WITH_RYE_BREAD =
            registerFood("lablab_bean_soup_with_rye_bread", FoodValues.LABLAB_BEAN_SOUP_WITH_RYE_BREAD);

    public static final DeferredItem<Item> WEISSWURST_WITH_BAGEL =
            registerFood("weisswurst_with_bagel", FoodValues.WEISSWURST_WITH_BAGEL);

    public static final DeferredItem<Item> BLACK_FOREST_GATEAU =
            registerFood("black_forest_gateau", FoodValues.BLACK_FOREST_GATEAU);

    public static final DeferredItem<Item> NAVAL_CURRY =
            registerFood("naval_curry", FoodValues.NAVAL_CURRY);

    public static final DeferredItem<Item> YOKAN =
            registerFood("yokan", FoodValues.YOKAN);

    public static final DeferredItem<Item> CHICKEN_TATSUTA_AGE =
            registerFood("chicken_tatsuta_age", FoodValues.CHICKEN_TATSUTA_AGE);

    public static final DeferredItem<Item> GRILLED_FISH_MISO_SOUP =
            registerFood("grilled_fish_miso_soup", FoodValues.GRILLED_FISH_MISO_SOUP);

    public static final DeferredItem<Item> TEMPURA_SOBA_NOODLES =
            registerFood("tempura_soba_noodles", FoodValues.TEMPURA_SOBA_NOODLES);

    public static final DeferredItem<Item> MISO_SOUP =
            registerFood("miso_soup", FoodValues.MISO_SOUP);

    public static final DeferredItem<Item> SOBA_NOODLES =
            registerFood("soba_noodles", FoodValues.SOBA_NOODLES);

    public static final DeferredItem<Item> FISH_TEMPURA =
            registerFood("fish_tempura", FoodValues.FISH_TEMPURA);

    public static final DeferredItem<Item> TOFU_WITH_SALTED_EGG =
            registerFood("tofu_with_salted_egg", FoodValues.TOFU_WITH_SALTED_EGG);

    public static final DeferredItem<Item> SCONE =
            registerFood("scone", FoodValues.SCONE);

    public static final DeferredItem<Item> EGG_SANDWICH =
            registerFood("egg_sandwich", FoodValues.EGG_SANDWICH);

    public static final DeferredItem<Item> BACON_SANDWICH =
            registerFood("bacon_sandwich", FoodValues.BACON_SANDWICH);

    public static final DeferredItem<Item> YORKSHIRE_PUDDING =
            registerFood("yorkshire_pudding", FoodValues.YORKSHIRE_PUDDING);

    public static final DeferredItem<Item> VENETIAN_CUTTLEFISH_NOODLES =
            registerFood("venetian_cuttlefish_noodles", FoodValues.VENETIAN_CUTTLEFISH_NOODLES);

    // 3.1 矿物资源
    public static final DeferredItem<Item> BAUXITE =
            ITEMS.registerSimpleItem("bauxite");

    public static final DeferredItem<Item> ALUMINUM_INGOT =
            ITEMS.registerSimpleItem("aluminum_ingot");

    public static final DeferredItem<Item> ALUMINUM_NUGGET =
            ITEMS.registerSimpleItem("aluminum_nugget");

    public static final DeferredItem<Item> SALTPETER =
            ITEMS.registerSimpleItem("saltpeter");

    public static final DeferredItem<Item> STEEL_INGOT =
            ITEMS.registerSimpleItem("steel_ingot");

    public static final DeferredItem<Item> STEEL_NUGGET =
            ITEMS.registerSimpleItem("steel_nugget");

    public static final DeferredItem<BlockItem> BAUXITE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_ORE);

    public static final DeferredItem<BlockItem> BAUXITE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.BAUXITE_BLOCK);

    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ALUMINUM_BLOCK);

    public static final DeferredItem<BlockItem> SALTPETER_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.SALTPETER_BLOCK);

    public static final DeferredItem<BlockItem> STEEL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.STEEL_BLOCK);

    public static final DeferredItem<FuelBarrelItem> FUEL_BARREL_ITEM =
            ITEMS.register("fuel_barrel",
                    () -> new FuelBarrelItem(ModBlocks.FUEL_BARREL.get(), new Item.Properties()));

    public static final DeferredItem<BucketItem> CRUDE_OIL_BUCKET =
            ITEMS.register("crude_oil_bucket",
                    () -> new BucketItem(ModFluids.CRUDE_OIL.get(),
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    public static final DeferredItem<BucketItem> FUEL_OIL_BUCKET =
            ITEMS.register("fuel_oil_bucket",
                    () -> new BucketItem(ModFluids.FUEL_OIL.get(),
                            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static DeferredItem<Item> registerFood(String name, FoodProperties food) {
        return ITEMS.registerSimpleItem(name, new Item.Properties().stacksTo(16).food(food));
    }
}
