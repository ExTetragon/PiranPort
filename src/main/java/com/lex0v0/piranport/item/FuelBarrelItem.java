package com.lex0v0.piranport.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/** 流体恢复由 vanilla 的 BlockEntity 数据组件流程在物品消耗前完成。 */
public class FuelBarrelItem extends BlockItem {
    public FuelBarrelItem(Block block, Properties properties) {
        super(block, properties);
    }
}
