package me.tlwsy.bypass.mixin;

import me.tlwsy.bypass.BypassFabricCheck;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class BypassFabricCheckMixin {

    @Unique
    private static final Logger BYPASS_LOGGER = LoggerFactory.getLogger("BypassCheck");
    
    @Unique
    private static Field currentTaskField;
    @Unique
    private static Method finishTaskMethod;
    @Unique
    private static boolean reflectionFailed = false;

    @Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void interceptDisconnect(Text reason, CallbackInfo ci) {
        if (!BypassFabricCheck.IS_ENABLED) return;

        if (!((Object) this instanceof ServerConfigurationNetworkHandler handler)) return;

        String message = reason.getString();
        if (message.contains("Fabric") && (message.contains("requires") || message.contains("install"))) {
            BYPASS_LOGGER.warn("[BypassCheck] Intercepted Fabric handshake failure, attempting graceful bypass...");
            
            ci.cancel();
            this.completeTaskAndProgress(handler);
        }
    }

    @Unique
    private void completeTaskAndProgress(ServerConfigurationNetworkHandler handler) {
        try {
            if (currentTaskField == null && !reflectionFailed) {
                prepareReflection();
            }

            if (reflectionFailed) {
                BYPASS_LOGGER.error("[BypassCheck] Reflection failed, cannot bypass safely.");
                return;
            }

            // 获取当前任务
            Object currentTask = currentTaskField.get(handler);
            if (currentTask != null) {
                // 获取任务类型 (ConfigurationTask.Type)
                Method typeMethod = currentTask.getClass().getMethod("type");
                Object taskType = typeMethod.invoke(currentTask);

                // 调用 finishTask(type) 结束当前任务
                // 这会触发 pollNextTask()，从而正常推进到下一个任务或 Play 阶段
                finishTaskMethod.setAccessible(true);
                finishTaskMethod.invoke(handler, taskType);
                
                BYPASS_LOGGER.info("[BypassCheck] Successfully finished current task: {}", taskType);
            } else {
                // 如果当前没有任务，尝试直接进入游戏状态
                try {
                    Method switchToPlay = handler.getClass().getDeclaredMethod("switchToPlay");
                    switchToPlay.setAccessible(true);
                    switchToPlay.invoke(handler);
                } catch (Exception e) {
                    BYPASS_LOGGER.error("[BypassCheck] No active task and failed to switch to play", e);
                }
            }
        } catch (Exception e) {
            BYPASS_LOGGER.error("[BypassCheck] Failed to bypass configuration task", e);
        }
    }

    @Unique
    private void prepareReflection() {
        try {
            Class<?> clazz = ServerConfigurationNetworkHandler.class;
            
            // 尝试查找 currentTask 字段 (Intermediary: field_45030)
            try {
                currentTaskField = clazz.getDeclaredField("currentTask");
            } catch (NoSuchFieldException e) {
                currentTaskField = clazz.getDeclaredField("field_45030");
            }
            currentTaskField.setAccessible(true);

            // 尝试查找 finishTask 方法 (Intermediary: method_52408)
            // 该方法接受一个 ConfigurationTask.Type 参数
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("finishTask") || m.getName().equals("method_52408")) {
                    finishTaskMethod = m;
                    break;
                }
            }

            if (currentTaskField == null || finishTaskMethod == null) {
                reflectionFailed = true;
                BYPASS_LOGGER.error("[BypassCheck] Could not find required fields/methods for bypass.");
            }
        } catch (Exception e) {
            reflectionFailed = true;
            BYPASS_LOGGER.error("[BypassCheck] Reflection preparation failed", e);
        }
    }
}