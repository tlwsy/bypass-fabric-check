package name.modid.mixin;

import name.modid.BypassFabricCheck; // 引入主类以访问开关
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

		// 【新增】 检查全局开关！
		// 如果玩家用指令关闭了绕过功能 (false)，则直接返回，不执行任何拦截。
		// 这样就相当于模组不存在，游戏会执行原版的“断开连接”逻辑。
		if (!BypassFabricCheck.IS_ENABLED) {
			return;
		}

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
				// 4. 清理现场 (排除法)
				Class<?> clazz = this.getClass();

				for (Field field : clazz.getDeclaredFields()) {
					field.setAccessible(true);
					Object value = field.get(this);
					if (value == null) continue;

					String typeName = field.getType().getName();

					// 白名单
					if (typeName.contains("GameProfile") || typeName.contains("Logger") ||
							typeName.contains("MinecraftServer") || field.getType().isPrimitive() ||
							value instanceof String) {
						continue;
					}

					// 清空集合
					if (value instanceof Collection) {
						((Collection<?>) value).clear();
						continue;
					}
					if (value instanceof Map) {
						((Map<?, ?>) value).clear();
						continue;
					}

					// 置空不明对象 (Task)
					field.set(this, null);
				}

				// 5. 强制进入游戏
				Method method = null;
				String[] possibleNames = {"switchToPlay", "method_52409", "startPlay", "finishConfiguration"};

				for (String name : possibleNames) {
					try {
						method = clazz.getDeclaredMethod(name);
						break;
					} catch (NoSuchMethodException ignored) {}
				}

				if (method != null) {
					method.setAccessible(true);
					method.invoke(this);
					BYPASS_LOGGER.info("[BypassCheck] 成功跳转至 PLAY 阶段！");
				}

			} catch (Exception e) {
				BYPASS_LOGGER.error("[BypassCheck] 绕过逻辑异常: ", e);
			}
		}
	}
}