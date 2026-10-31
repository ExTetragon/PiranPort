package com.lex0v0.piranport;

import com.lex0v0.piranport.registry.ModBlocks;
import com.lex0v0.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// 通过实际战利品表验证工具、附魔与硝土数量边界。
@GameTestHolder(PiranPort.MODID)
@PrefixGameTestTemplate(false)
public final class MiningLootGameTests {
    @GameTest(template="empty")
    public static void saltpeterShovelsYieldFourAndOtherToolsTwoToFour(GameTestHelper h) {
        var block=ModBlocks.SALTPETER_BLOCK.get();var pos=new BlockPos(4,2,4);h.setBlock(pos,block);
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        for(var item:java.util.List.of(Items.WOODEN_SHOVEL,Items.STONE_SHOVEL,Items.IRON_SHOVEL,Items.GOLDEN_SHOVEL,Items.DIAMOND_SHOVEL,Items.NETHERITE_SHOVEL)) {
            var tool=new ItemStack(item);tool.enchant(registry.getHolderOrThrow(Enchantments.FORTUNE),10);
            for(int i=0;i<32;i++) {
                var drops=Block.getDrops(block.defaultBlockState(),h.getLevel(),h.absolutePos(pos),null,null,tool);
                h.assertTrue(drops.size()==1&&drops.getFirst().is(ModItems.SALTPETER.get())&&drops.getFirst().getCount()==4,"every shovel yields exactly four, fortune does not inflate");
            }
            tool.enchant(registry.getHolderOrThrow(Enchantments.SILK_TOUCH),1);
            var silk=Block.getDrops(block.defaultBlockState(),h.getLevel(),h.absolutePos(pos),null,null,tool);
            h.assertTrue(silk.size()==1&&silk.getFirst().is(block.asItem())&&silk.getFirst().getCount()==1,"silk touch keeps priority over shovel quantity");
        }
        for(var tool:java.util.List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_PICKAXE),new ItemStack(Items.IRON_AXE))) {
            boolean belowFour=false;
            for(int i=0;i<128;i++) {
                var drops=Block.getDrops(block.defaultBlockState(),h.getLevel(),h.absolutePos(pos),null,null,tool);
                int amount=drops.stream().mapToInt(ItemStack::getCount).sum();
                h.assertTrue(drops.stream().allMatch(s->s.is(ModItems.SALTPETER.get()))&&amount>=2&&amount<=4,"unmatched tools retain two-to-four range");
                belowFour|=amount<4;
            }
            h.assertTrue(belowFour,"unmatched tools retain random quantities");
        }
        h.succeed();
    }
}
