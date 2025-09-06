package com.hbm.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.hbm.core"})
@IFMLLoadingPlugin.SortingIndex(2077) // mlbv: this shit must be greater than 1000, after the srg transformer
public class HbmCorePlugin implements IFMLLoadingPlugin {

    static final Logger coreLogger = LogManager.getLogger("HBM CoreMod");
    private static boolean runtimeDeobfEnabled = false;
    private static boolean hardCrash = true;

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{GlStateManagerTransformer.class.getName(), ContainerTransformer.class.getName(),
                InventoryPlayerTransformer.class.getName(), ForgeHooksTransformer.class.getName(), ChunkTransformer.class.getName(),
                WorldTransformer.class.getName(), WorldServerTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return "com.hbm.core.HbmCoreModContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        runtimeDeobfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
        String prop = System.getProperty("hbm.core.disablecrash");
        if (prop != null) {
            hardCrash = false;
            coreLogger.info("Crash suppressed with -Dhbm.core.disablecrash");
        }
    }

    static void fail(String className, Throwable t) {
        coreLogger.fatal("Error transforming class {}. This is a coremod clash! Please report this on our issue tracker", className, t);
        if (hardCrash) {
            coreLogger.info("Crashing! To suppress the crash, launch Minecraft with -Dhbm.core.disablecrash");
            throw new IllegalStateException("HBM CoreMod transformation failure: " + className, t);
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static boolean runtimeDeobfEnabled() {
        return runtimeDeobfEnabled;
    }
}
