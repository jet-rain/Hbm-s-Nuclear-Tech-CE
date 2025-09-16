package com.hbm.util;

import com.hbm.inventory.recipes.loader.SerializableRecipe;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ICrashCallable;

public class CrashHelper {

    public static void init() {
        FMLCommonHandler.instance().registerCrashCallable(new CrashCallableRecipe());
    }

    public static class CrashCallableRecipe implements ICrashCallable {

        @Override
        public String getLabel() {
            return "NTM Modified recipes";
        }

        @Override
        public String call() throws Exception {
            StringBuilder call = new StringBuilder();
            for(SerializableRecipe rec : SerializableRecipe.recipeHandlers) {
                if(rec.modified) call.append("\n\t\t").append(rec.getFileName());
            }
            return call.toString();
        }
    }
}
