package com.hbm.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static com.hbm.core.HbmCorePlugin.coreLogger;
import static com.hbm.core.HbmCorePlugin.fail;

public final class WorldServerTransformer implements IClassTransformer, Opcodes {
    private static final ObfSafeName TICK_WS = new ObfSafeName("tick", "func_72835_b");
    private static final ObfSafeName ICP_TICK = new ObfSafeName("tick", "func_73156_b");
    private static final ObfSafeName TICK_UPDATES = new ObfSafeName("tickUpdates", "func_72955_a");
    private static final ObfSafeName UPDATE_BLOCKS = new ObfSafeName("updateBlocks", "func_147456_g");
    private static final ObfSafeName CHUNK_PROVIDER = new ObfSafeName("chunkProvider", "field_73020_y");
    private static final String WORLDSERVER = "net/minecraft/world/WorldServer";

    private static boolean injectBeforeChunkProviderTick(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode min)) continue;

            boolean isIcpTick = (min.getOpcode() == INVOKEINTERFACE || min.getOpcode() == INVOKEVIRTUAL) && ICP_TICK.matches(min.name) &&
                                "()Z".equals(min.desc) && ("net/minecraft/world/chunk/IChunkProvider".equals(min.owner) ||
                                                           "net/minecraft/world/gen/ChunkProviderServer".equals(min.owner));

            if (!isIcpTick) continue;
            AbstractInsnNode getfield = skipBackNonOps(min.getPrevious());
            if (!(getfield instanceof FieldInsnNode fin) || getfield.getOpcode() != GETFIELD) continue;
            if (!CHUNK_PROVIDER.matches(fin.name)) continue;
            if (!"net/minecraft/world/World".equals(fin.owner) && !WORLDSERVER.equals(fin.owner)) continue;

            AbstractInsnNode aloadThis = skipBackNonOps(getfield.getPrevious());
            if (!(aloadThis instanceof VarInsnNode) || aloadThis.getOpcode() != ALOAD || ((VarInsnNode) aloadThis).var != 0) continue;
            mn.instructions.insertBefore(aloadThis, callWorldServerHook("onUpdateChunkProvider"));
            return true;
        }
        return false;
    }

    private static boolean injectBeforeTickUpdates(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode min)) continue;

            boolean isTickUpdates =
                    min.getOpcode() == INVOKEVIRTUAL && WORLDSERVER.equals(min.owner) && TICK_UPDATES.matches(min.name) && "(Z)Z".equals(min.desc);

            if (!isTickUpdates) continue;
            AbstractInsnNode argPush = skipBackNonOps(min.getPrevious());
            AbstractInsnNode aloadThis = skipBackNonOps(argPush != null ? argPush.getPrevious() : null);
            if (!(aloadThis instanceof VarInsnNode) || aloadThis.getOpcode() != ALOAD || ((VarInsnNode) aloadThis).var != 0) continue;

            mn.instructions.insertBefore(aloadThis, callWorldServerHook("onUpdatePendingBlocks"));
            return true;
        }
        return false;
    }

    private static boolean injectBeforeUpdateBlocks(MethodNode mn) {
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (!(insn instanceof MethodInsnNode min)) continue;

            boolean isUpdateBlocks =
                    min.getOpcode() == INVOKEVIRTUAL && WORLDSERVER.equals(min.owner) && UPDATE_BLOCKS.matches(min.name) && "()V".equals(min.desc);

            if (!isUpdateBlocks) continue;

            AbstractInsnNode aloadThis = skipBackNonOps(min.getPrevious());
            if (!(aloadThis instanceof VarInsnNode) || aloadThis.getOpcode() != ALOAD || ((VarInsnNode) aloadThis).var != 0) continue;

            mn.instructions.insertBefore(aloadThis, callWorldServerHook("onUpdateBlocks"));
            return true;
        }
        return false;
    }

    private static InsnList callWorldServerHook(String method) {
        InsnList il = new InsnList();
        il.add(new VarInsnNode(ALOAD, 0));
        il.add(new MethodInsnNode(INVOKESTATIC, "com/hbm/core/WorldHook", method, "(Lnet/minecraft/world/WorldServer;)V", false));
        return il;
    }

    private static AbstractInsnNode skipBackNonOps(AbstractInsnNode from) {
        AbstractInsnNode p = from;
        while (p != null &&
               (p.getType() == AbstractInsnNode.LABEL || p.getType() == AbstractInsnNode.FRAME || p.getType() == AbstractInsnNode.LINE)) {
            p = p.getPrevious();
        }
        return p;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.world.WorldServer".equals(transformedName)) return basicClass;

        try {
            ClassNode cn = new ClassNode();
            new ClassReader(basicClass).accept(cn, 0);

            boolean patched = false;

            for (MethodNode mn : cn.methods) {
                if (!TICK_WS.matches(mn.name) || !"()V".equals(mn.desc)) continue;

                coreLogger.info("Patching WorldServer#{}{}", mn.name, mn.desc);

                boolean a = injectBeforeChunkProviderTick(mn);
                boolean b = injectBeforeTickUpdates(mn);
                boolean c = injectBeforeUpdateBlocks(mn);

                coreLogger.info("WorldServer.tick hooks: chunkProvider={}, tickUpdates={}, updateBlocks={}", a, b, c);
                patched = a && b && c;
                break;
            }

            if (!patched) throw new IllegalStateException("Failed to inject one or more hooks into WorldServer#tick");

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            return cw.toByteArray();

        } catch (Throwable t) {
            fail("net.minecraft.world.WorldServer", t);
            return basicClass;
        }
    }
}
