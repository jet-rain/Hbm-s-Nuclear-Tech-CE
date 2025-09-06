package com.hbm.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static com.hbm.core.HbmCorePlugin.coreLogger;
import static com.hbm.core.HbmCorePlugin.fail;
import static org.objectweb.asm.Opcodes.*;

public final class WorldTransformer implements IClassTransformer {
    private static final ObfSafeName UPDATE_ENTITIES = new ObfSafeName("updateEntities", "func_72939_s");
    private static final ObfSafeName PROCESSING_LOADED_TILES = new ObfSafeName("processingLoadedTiles", "field_147481_N");

    private static void injectHead(MethodNode mn) {
        InsnList hook = callWorldHook("onUpdateEntities");
        mn.instructions.insert(hook);
    }

    private static int injectTailReturns(MethodNode mn) {
        int count = 0;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == RETURN) {
                mn.instructions.insertBefore(insn, callWorldHook("onUpdateEntitiesEnd"));
                count++;
            }
        }
        return count;
    }

    private static boolean injectBeforeProcessingTrue(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == PUTFIELD && insn instanceof FieldInsnNode fin) {
                if (!PROCESSING_LOADED_TILES.matches(fin.name)) continue;
                AbstractInsnNode prev = skipBackNonOps(fin);
                if (prev != null && isPushConst(prev, 1)) {
                    mn.instructions.insertBefore(prev, callWorldHook("onTileEntityProcessingStart"));
                    return true;
                }
            }
        }
        coreLogger.fatal("Did not find processingLoadedTiles = true site");
        return false;
    }

    private static boolean injectAfterProcessingFalse(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == PUTFIELD && insn instanceof FieldInsnNode fin) {
                if (!PROCESSING_LOADED_TILES.matches(fin.name)) continue;
                AbstractInsnNode prev = skipBackNonOps(fin);
                if (prev != null && isPushConst(prev, 0)) {
                    mn.instructions.insert(insn, callWorldHook("onTileEntityProcessingEnd"));
                    return true;
                }
            }
        }
        coreLogger.fatal("Did not find processingLoadedTiles = false site");
        return false;
    }

    private static InsnList callWorldHook(String method) {
        InsnList il = new InsnList();
        il.add(new VarInsnNode(ALOAD, 0));
        il.add(new MethodInsnNode(INVOKESTATIC, "com/hbm/core/WorldHook", method, "(Lnet/minecraft/world/World;)V", false));
        return il;
    }

    private static AbstractInsnNode skipBackNonOps(AbstractInsnNode from) {
        AbstractInsnNode p = from.getPrevious();
        while (p != null &&
               (p.getType() == AbstractInsnNode.LABEL || p.getType() == AbstractInsnNode.FRAME || p.getType() == AbstractInsnNode.LINE)) {
            p = p.getPrevious();
        }
        return p;
    }

    private static boolean isPushConst(AbstractInsnNode insn, int value) {
        int op = insn.getOpcode();
        if (value == 0) return op == ICONST_0;
        if (value == 1) return op == ICONST_1;
        // extremely defensive (shouldn't occur for booleans)
        if (op == BIPUSH) return ((IntInsnNode) insn).operand == value;
        if (op == SIPUSH) return ((IntInsnNode) insn).operand == value;
        return false;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals("net.minecraft.world.World")) return basicClass;
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(basicClass).accept(cn, 0);

            boolean allPatched = false;

            for (MethodNode mn : cn.methods) {
                if (!UPDATE_ENTITIES.matches(mn.name) || !"()V".equals(mn.desc)) continue;

                coreLogger.info("Patching World#{} / {}{}", UPDATE_ENTITIES.mcp, mn.name, mn.desc);
                injectHead(mn);
                int returnsPatched = injectTailReturns(mn);
                boolean startOk = injectBeforeProcessingTrue(mn);
                boolean endOk = injectAfterProcessingFalse(mn);

                coreLogger.info("World.updateEntities hooks: head=ok, start={}, end={}, returns={}", startOk, endOk, returnsPatched);
                allPatched = returnsPatched > 0 && startOk && endOk;
                break;
            }

            if (!allPatched) {
                throw new IllegalStateException("One or more transforms failed to apply");
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();

        } catch (Throwable t) {
            fail("net.minecraft.world.World", t);
            return basicClass;
        }
    }
}
