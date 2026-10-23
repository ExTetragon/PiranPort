package com.lex0v0.piranport.registry;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

// 集中定义基础食物值并换算饱和系数，特殊效果尚未接入。
public final class FoodValues {
    public static final FoodProperties TAPTAP_ICE_CREAM = food(1, 1F);
    public static final FoodProperties INSTANT_NOODLES = food(12, 16F);
    public static final FoodProperties TOAST_BREAD = food(15, 18.8F);
    public static final FoodProperties NAVY_BAKED_BEANS = food(4, 5F);
    public static final FoodProperties SURSTROMMING = food(4, 5F);
    public static final FoodProperties LATIAO = food(2, 2.5F);
    public static final FoodProperties ASSORTED_CHAR_SIU_FRIED_RICE = food(10, 12.5F);
    public static final FoodProperties DELUXE_BAOZI = food(10, 12.5F);
    public static final FoodProperties SWEET_TOFU_PUDDING = food(3, 3.8F, Items.BOWL);
    public static final FoodProperties SAVORY_TOFU_PUDDING = food(3, 3.8F, Items.BOWL);
    public static final FoodProperties DONGPO_PORK = food(6, 7.5F);
    public static final FoodProperties MAPO_TOFU = food(4, 5F);
    public static final FoodProperties TANGYUAN = food(6, 7.5F);
    public static final FoodProperties BOUILLABAISSE = food(7, 8.8F, Items.BOWL);
    public static final FoodProperties TARTE_TATIN = food(5, 6.3F);
    public static final FoodProperties MACARON = food(2, 2.5F);
    public static final FoodProperties RYE_BREAD = food(4, 5F);
    public static final FoodProperties FRESH_RYE_BREAD = food(4, 5F);
    public static final FoodProperties PELMENI = food(6, 7.5F);
    public static final FoodProperties KHOLODETS = food(6, 7.5F);
    public static final FoodProperties BORSCHT = food(20, 25F, Items.BOWL);
    public static final FoodProperties KVASS = food(4, 5F, Items.GLASS_BOTTLE);
    public static final FoodProperties PLATED_ROYAL_NAVAL_SALTED_BEEF = food(8, 10F, Items.BOWL);
    public static final FoodProperties BLACK_TEA_SANDWICH = food(5, 6.3F);
    public static final FoodProperties BLACK_TEA_SCONE = food(5, 6.3F);
    public static final FoodProperties FISH_AND_CHIPS = food(5, 6.3F);
    public static final FoodProperties STARGAZY_PIE = food(6, 7.5F);
    public static final FoodProperties AMERICAN_BURGER = food(8, 10F);
    public static final FoodProperties DOUBLE_AMERICAN_BURGER = food(12, 15F);
    public static final FoodProperties TRIPLE_AMERICAN_BURGER = food(16, 20F);
    public static final FoodProperties EGGS_BENEDICT = food(4, 5F);
    public static final FoodProperties HOT_DOG = food(4, 5F);
    public static final FoodProperties BARBECUE = food(5, 6.3F);
    public static final FoodProperties APPLE_PIE = food(5, 6.3F);
    public static final FoodProperties TORPEDO_JUICE = food(3, 3.8F);
    public static final FoodProperties SALAMI_PIZZA = food(24, 30F);
    public static final FoodProperties SALAMI_PIZZA_SLICES = food(3, 3.8F);
    public static final FoodProperties BOLOGNESE_LINGUINE = food(8, 10F);
    public static final FoodProperties MUSSOLINIS_OO = food(10, 12.5F);
    public static final FoodProperties THURINGIAN_ROSTBRATWURST_WITH_BEER = food(8, 10F, Items.GLASS_BOTTLE);
    public static final FoodProperties THURINGIAN_ROSTBRATWURST = food(7, 8.8F);
    public static final FoodProperties SCHWEINSHAXE = food(8, 10F);
    public static final FoodProperties LABLAB_BEAN_SOUP_WITH_RYE_BREAD = food(9, 11.3F, Items.BOWL);
    public static final FoodProperties WEISSWURST_WITH_BAGEL = food(7, 8.8F);
    public static final FoodProperties BLACK_FOREST_GATEAU = food(6, 7.5F);
    public static final FoodProperties NAVAL_CURRY = food(5, 6.3F);
    public static final FoodProperties YOKAN = food(3, 3.8F);
    public static final FoodProperties CHICKEN_TATSUTA_AGE = food(6, 7.5F);
    public static final FoodProperties GRILLED_FISH_MISO_SOUP = food(6, 7.5F, Items.BOWL);
    public static final FoodProperties TEMPURA_SOBA_NOODLES = food(6, 7.5F);
    public static final FoodProperties MISO_SOUP = food(6, 7.5F, Items.BOWL);
    public static final FoodProperties SOBA_NOODLES = food(6, 7.5F);
    public static final FoodProperties FISH_TEMPURA = food(6, 7.5F);
    public static final FoodProperties TOFU_WITH_SALTED_EGG = food(3, 3.8F);
    public static final FoodProperties SCONE = food(3, 3.8F);
    public static final FoodProperties EGG_SANDWICH = food(5, 6.3F);
    public static final FoodProperties BACON_SANDWICH = food(6, 7.5F);
    public static final FoodProperties YORKSHIRE_PUDDING = food(4, 5F);
    public static final FoodProperties VENETIAN_CUTTLEFISH_NOODLES = food(7, 8.8F);

    private FoodValues() {
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return food(nutrition, saturation, null);
    }

    private static FoodProperties food(int nutrition, float saturation, ItemLike container) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation / (nutrition * 2.0F));
        if (container != null) {
            builder.usingConvertsTo(container);
        }
        return builder.build();
    }
}
