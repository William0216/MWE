package fr.alexdoru.megawallsenhancementsmod.asm.transformers;

import fr.alexdoru.megawallsenhancementsmod.asm.ASMLoadingPlugin;
import fr.alexdoru.megawallsenhancementsmod.asm.IMyClassTransformer;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class RenderManagerTransformer implements IMyClassTransformer {

    @Override
    public String getTargetClassName() {
        return "net.minecraft.client.renderer.entity.RenderManager";
    }

    @Override
    public ClassNode transform(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(ASMLoadingPlugin.isObf ? "b" : "renderDebugBoundingBox") && methodNode.desc.equals(ASMLoadingPlugin.isObf ? "(Lpk;DDDFF)V" : "(Lnet/minecraft/entity/Entity;DDDFF)V")) {

                /*
                 * Injects at head :
                 * if(RenderManagerHook.cancelHitboxRender(entityIn)) {
                 *     return;
                 * }
                 */
                methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), getCancelRenderInsnList());

                for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {

                    if (insnNode.getOpcode() == INSTANCEOF && insnNode instanceof TypeInsnNode && ((TypeInsnNode) insnNode).desc.equals(ASMLoadingPlugin.isObf ? "pr" : "net/minecraft/entity/EntityLivingBase")) {
                        AbstractInsnNode nextNode = insnNode.getNext();
                        if (nextNode.getOpcode() == IFEQ && nextNode instanceof JumpInsnNode) {
                            LabelNode labelNode = ((JumpInsnNode) nextNode).label;
                            /*
                             * Transforms line 453 :
                             * if (entityIn instanceof EntityLivingBase)
                             * Becomes :
                             * if (entityIn instanceof EntityLivingBase && ConfigHandler.drawRedBox)
                             */
                            InsnList list = new InsnList();
                            list.add(new JumpInsnNode(IFEQ, labelNode));
                            list.add(new FieldInsnNode(GETSTATIC, "fr/alexdoru/megawallsenhancementsmod/config/ConfigHandler", "drawRedBox", "Z"));
                            methodNode.instructions.insertBefore(nextNode, list);
                        }

                    }

                    if (insnNode instanceof LdcInsnNode && ((LdcInsnNode) insnNode).cst.equals(new Double("2.0"))) {
                        /*
                         * Line 464
                         * Replaces the 2.0D with RenderManagerHook.getBlueVectLength(entityIn);
                         */
                        InsnList list = new InsnList();
                        list.add(new VarInsnNode(ALOAD, 1)); // load entity
                        list.add(new MethodInsnNode(INVOKESTATIC, "fr/alexdoru/megawallsenhancementsmod/asm/hooks/RenderManagerHook", "getBlueVectLength", ASMLoadingPlugin.isObf ? "(Lpk;)D" : "(Lnet/minecraft/entity/Entity;)D", false));
                        methodNode.instructions.insertBefore(insnNode, list);
                        methodNode.instructions.remove(insnNode);
                    }
                }

            }
        }
        return classNode;
    }

    private InsnList getCancelRenderInsnList() {
        InsnList list = new InsnList();
        LabelNode notCancelled = new LabelNode();
        list.add(new VarInsnNode(ALOAD, 1)); // load entity
        list.add(new MethodInsnNode(INVOKESTATIC, "fr/alexdoru/megawallsenhancementsmod/asm/hooks/RenderManagerHook", "cancelHitboxRender", ASMLoadingPlugin.isObf ? "(Lpk;)Z" : "(Lnet/minecraft/entity/Entity;)Z", false)); // load the boolean
        list.add(new JumpInsnNode(IFEQ, notCancelled)); // if (true) { return;} else {jump to notCancelled label}
        list.add(new InsnNode(RETURN)); // return;
        list.add(notCancelled);
        return list;
    }

}