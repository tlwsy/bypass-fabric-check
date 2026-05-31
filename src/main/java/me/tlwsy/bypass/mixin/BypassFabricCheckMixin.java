package me.tlwsy.bypass.mixin;

import me.tlwsy.bypass.BypassFabricCheck;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class BypassFabricCheckMixin {

    @Unique
    private static final Logger BYPASS_LOGGER = LoggerFactory.getLogger("BypassCheck");

    @Unique
    private static Field currentTaskField;
    @Unique
    private static Method finishCurrentTaskMethod;
    @Unique
    private static boolean reflectionFailed = false;

    @Inject(method = "disconnect(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"), cancellable = true)
    private void interceptDisconnect(Component reason, CallbackInfo ci) {
        if (!BypassFabricCheck.IS_ENABLED) return;

        if (!((Object) this instanceof ServerConfigurationPacketListenerImpl handler)) return;

        String message = reason.getString();
        if (message.contains("Fabric") && (message.contains("requires") || message.contains("install"))) {
            BYPASS_LOGGER.warn("[BypassCheck] Intercepted Fabric handshake failure, attempting graceful bypass...");

            ci.cancel();
            this.completeTaskAndProgress(handler);
        }
    }

    @Unique
    private void completeTaskAndProgress(ServerConfigurationPacketListenerImpl handler) {
        try {
            if (currentTaskField == null && !reflectionFailed) {
                prepareReflection();
            }

            if (reflectionFailed) {
                BYPASS_LOGGER.error("[BypassCheck] Reflection failed, cannot bypass safely.");
                return;
            }

            Object currentTask = currentTaskField.get(handler);
            if (currentTask != null) {
                Method typeMethod = currentTask.getClass().getMethod("type");
                Object taskType = typeMethod.invoke(currentTask);

                finishCurrentTaskMethod.setAccessible(true);
                finishCurrentTaskMethod.invoke(handler, taskType);

                BYPASS_LOGGER.info("[BypassCheck] Successfully finished current task: {}", taskType);
            } else {
                BYPASS_LOGGER.warn("[BypassCheck] No active task found, configuration may proceed naturally.");
            }
        } catch (Exception e) {
            BYPASS_LOGGER.error("[BypassCheck] Failed to bypass configuration task", e);
        }
    }

    @Unique
    private void prepareReflection() {
        try {
            Class<?> clazz = ServerConfigurationPacketListenerImpl.class;

            try {
                currentTaskField = clazz.getDeclaredField("currentTask");
            } catch (NoSuchFieldException e) {
                BYPASS_LOGGER.error("[BypassCheck] Could not find currentTask field");
                reflectionFailed = true;
                return;
            }
            currentTaskField.setAccessible(true);

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("finishCurrentTask")) {
                    finishCurrentTaskMethod = m;
                    break;
                }
            }

            if (finishCurrentTaskMethod == null) {
                BYPASS_LOGGER.error("[BypassCheck] Could not find finishCurrentTask method");
                reflectionFailed = true;
                return;
            }
        } catch (Exception e) {
            reflectionFailed = true;
            BYPASS_LOGGER.error("[BypassCheck] Reflection preparation failed", e);
        }
    }
}
