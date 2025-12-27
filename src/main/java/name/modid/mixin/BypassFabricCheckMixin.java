package name.modid.mixin;

import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Queue;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class BypassFabricCheckMixin {

	private static final Logger BYPASS_LOGGER = LoggerFactory.getLogger("BypassCheck");

	@Inject(method = "disconnect(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
	private void interceptDisconnect(Text reason, CallbackInfo ci) {

		// 1. 作用域检查
		if (!((Object) this instanceof ServerConfigurationNetworkHandler)) {
			return;
		}

		String message = reason.getString();

		// 2. 关键词检查
		if (message.contains("Fabric") && (message.contains("requires") || message.contains("install"))) {

			BYPASS_LOGGER.info("[BypassCheck] 拦截到 Fabric 协议检查: {}", message);

			// 3. 阻止断开
			ci.cancel();

			try {
				// 4. 【核心修复】 纯反射清理任务 (不引用任何 Minecraft 类)
				Class<?> clazz = this.getClass();

				for (Field field : clazz.getDeclaredFields()) {
					field.setAccessible(true);

					// 获取字段类型的名称
					String typeName = field.getType().getSimpleName();
					Object value = field.get(this);

					// A. 清理队列 (Queue) - 存放待办任务的地方
					// 只要字段类型实现了 Queue 接口，或者名字里带 Queue，就清空它
					if (Queue.class.isAssignableFrom(field.getType())) {
						if (value != null) {
							((Queue<?>) value).clear();
							BYPASS_LOGGER.info("[BypassCheck] 已清空队列字段: {}", field.getName());
						}
					}

					// B. 清理当前任务 (ConfigurationTask)
					// 因为编译器找不到 ConfigurationTask 类，我们检查字段类型的名字是否包含 "Task"
					// 并且确保该字段当前有值 (value != null)
					if (value != null && (typeName.contains("ConfigurationTask") || typeName.contains("Task"))) {
						field.set(this, null);
						BYPASS_LOGGER.info("[BypassCheck] 已强制移除卡住的任务: {} (类型: {})", field.getName(), typeName);
					}
				}

				// 5. 强制进入游戏
				BYPASS_LOGGER.warn("[BypassCheck] 正在尝试强制进入游戏...");
				Method method = null;

				String[] possibleNames = {
						"switchToPlay",
						"method_52409",
						"startPlay",
						"finishConfiguration"
				};

				for (String name : possibleNames) {
					try {
						method = clazz.getDeclaredMethod(name);
						BYPASS_LOGGER.info("[BypassCheck] 找到切换方法: {}", name);
						break;
					} catch (NoSuchMethodException ignored) {}
				}

				if (method != null) {
					method.setAccessible(true);
					method.invoke(this);
					BYPASS_LOGGER.info("[BypassCheck] 成功跳转至 PLAY 阶段！");
				} else {
					BYPASS_LOGGER.error("[BypassCheck] 致命错误：无法通过反射找到切换方法！");
				}

			} catch (Exception e) {
				BYPASS_LOGGER.error("[BypassCheck] 绕过逻辑发生异常: ", e);
			}
		}
	}
}