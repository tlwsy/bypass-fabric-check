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
import java.util.Collection;
import java.util.Map;

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
				// 4. 【终极修复】 排除法清理现场
				// 我们不找 "Task" 了，直接把所有不认识的字段全部干掉
				Class<?> clazz = this.getClass();

				// 只获取当前类定义的字段 (不包括父类，这样比较安全)
				for (Field field : clazz.getDeclaredFields()) {
					field.setAccessible(true);
					Object value = field.get(this);
					String fieldName = field.getName();
					String typeName = field.getType().getName(); // 全限定类名

					// 调试日志：打印所有字段，方便排查
					// BYPASS_LOGGER.info("扫描字段: {} 类型: {} 值: {}", fieldName, typeName, value);

					if (value == null) continue;

					// A. 安全名单 (绝对不能删的东西)
					// GameProfile (玩家档案), Logger (日志), String (字符串), 基本类型
					if (typeName.contains("GameProfile") ||
							typeName.contains("Logger") ||
							typeName.contains("MinecraftServer") ||
							field.getType().isPrimitive() ||
							value instanceof String) {
						continue;
					}

					// B. 集合类型 (Queue, List, Map) -> 统统清空
					// 这里的 Queue 存放着待办任务，必须清空
					if (value instanceof Collection) {
						((Collection<?>) value).clear();
						BYPASS_LOGGER.info("[BypassCheck] 已清空集合/队列: {}", fieldName);
						continue;
					}
					if (value instanceof Map) {
						((Map<?, ?>) value).clear();
						BYPASS_LOGGER.info("[BypassCheck] 已清空Map: {}", fieldName);
						continue;
					}

					// C. 剩下的所有不明对象 -> 全部设为 NULL
					// 这里面一定包含那个卡住的 currentTask (synchronize_registries)
					field.set(this, null);
					BYPASS_LOGGER.info("[BypassCheck] 已移除不明对象 (目标锁定): {}", fieldName);
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