package com.lex0v0.piranport.blockentity.ammunitionfactory;

import com.lex0v0.piranport.recipe.AmmunitionFactoryRecipe;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

// 整计划冻结配方，当前批次与成功入库次数分别保存。
public final class AmmunitionJob {
    public final UUID id;
    public final ResourceLocation recipeId;
    public final AmmunitionFactoryRecipe recipe;
    public final int totalRuns;
    public int completedRuns,elapsed;
    public boolean inFlight,stopping;
    public AmmunitionJob(ResourceLocation recipeId,AmmunitionFactoryRecipe recipe,int runs){this.id=UUID.randomUUID();this.recipeId=recipeId;this.recipe=recipe;this.totalRuns=runs;}
    private AmmunitionJob(UUID id,ResourceLocation recipeId,AmmunitionFactoryRecipe recipe,int runs){this.id=id;this.recipeId=recipeId;this.recipe=recipe;this.totalRuns=runs;}
    public int totalPieces(){return totalRuns*recipe.count();}
    public int completedPieces(){return completedRuns*recipe.count();}
    public boolean finishedBatch(){return inFlight&&elapsed>=recipe.ticks();}
    public CompoundTag save(){
        CompoundTag t=new CompoundTag();t.putUUID("Id",id);t.putString("RecipeId",recipeId.toString());t.put("Recipe",recipe.save());t.putInt("TotalRuns",totalRuns);
        t.putInt("CompletedRuns",completedRuns);t.putInt("Elapsed",elapsed);t.putBoolean("InFlight",inFlight);t.putBoolean("Stopping",stopping);return t;
    }
    public static AmmunitionJob load(CompoundTag t){
        var recipe=AmmunitionFactoryRecipe.load(t.getCompound("Recipe"));var recipeId=ResourceLocation.tryParse(t.getString("RecipeId"));
        if(recipe==null||recipeId==null||!t.hasUUID("Id"))return null;
        int runs=t.getInt("TotalRuns"),done=t.getInt("CompletedRuns"),elapsed=t.getInt("Elapsed");boolean flight=t.getBoolean("InFlight"),stop=t.getBoolean("Stopping");
        if(runs<1||runs>Integer.MAX_VALUE/recipe.count()||done<0||done>=runs||elapsed<0||elapsed>recipe.ticks()||(!flight&&(elapsed!=0||stop)))return null;
        var job=new AmmunitionJob(t.getUUID("Id"),recipeId,recipe,runs);job.completedRuns=done;job.elapsed=elapsed;job.inFlight=flight;job.stopping=stop;return job;
    }
}
