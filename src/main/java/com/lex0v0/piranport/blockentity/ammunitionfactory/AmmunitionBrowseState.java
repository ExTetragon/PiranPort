package com.lex0v0.piranport.blockentity.ammunitionfactory;

import com.lex0v0.piranport.domain.ammunition.AmmunitionCategory;
import com.lex0v0.piranport.domain.ammunition.AmmunitionType;
import net.minecraft.nbt.CompoundTag;

// 每个窗口使用副本，最后关闭者写回机器，不保存玩家身份。
public final class AmmunitionBrowseState {
    public AmmunitionType selected=AmmunitionType.HIGH_EXPLOSIVE_SHELL;
    public int quantity=4;
    public boolean total;
    public AmmunitionCategory category(){return selected.category();}
    public CompoundTag save(){CompoundTag t=new CompoundTag();t.putString("Selected",selected.id().toString());t.putInt("Quantity",quantity);t.putBoolean("Total",total);return t;}
    public static AmmunitionBrowseState load(CompoundTag t){
        var s=new AmmunitionBrowseState();for(var type:AmmunitionType.values())if(type.id().toString().equals(t.getString("Selected")))s.selected=type;
        s.quantity=Math.max(0,t.getInt("Quantity"));s.total=t.getBoolean("Total");return s;
    }
    public AmmunitionBrowseState copy(){return load(save());}
}
