package com.hbm.config;

import net.minecraftforge.common.config.Configuration;

public class MachineConfig {

    protected static boolean scaleRTGPower = false;
    protected static boolean doRTGsDecay = true;
    protected static boolean disableMachines = false;
    //TODO: handle like on 1.7
    //mlbv: 1.7 hardcodes it to 6kB
    public static int crateByteSize = 8192;
    public static int rbmkJumpTemp = 1250;
    public static boolean enableOldTemplates = false;

    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_MACHINE = CommonConfig.CATEGORY_MACHINES;

        scaleRTGPower = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.01_scaleRTGPower", "Should RTG/Betavoltaic fuel power scale down as it decays?", false);
        doRTGsDecay = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.02_doRTGsDecay", "Should RTG/Betavoltaic fuel decay at all?", true);
        disableMachines = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.00_disableMachines", "Prevent mod from registering any Machines? (WARNING: THIS WILL BREAK PREEXISTING WORLDS)", false);
        enableOldTemplates = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.99_CE_01_enableOldTemplates", "Add deprecated assembler and chemfac templates to template folder.", false);
    }
}
