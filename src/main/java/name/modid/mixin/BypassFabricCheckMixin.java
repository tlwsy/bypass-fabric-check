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

import java.lang.reflect.Method;

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
			BYPASS_LOGGER.warn("[BypassCheck] 正在尝试通过反射强制进入游戏...");

			// 3. 阻止断开
			ci.cancel();

			// 4. 使用反射 (Reflection) 暴力调用切换方法
			try {
				// 获取当前类 (ServerConfigurationNetworkHandler)
				Class<?> clazz = this.getClass();
				Method method = null;

				// 尝试查找方法：为了兼容性，我们尝试多种可能的名称
				// 1.21.x 的核心方法通常没有参数
				String[] possibleNames = {
						"switchToPlay",     // Yarn 映射名 (开发环境)
						"method_52409",     // Intermediary 映射名 (Fabric 生产环境核心)
						"startPlay",        // Mojang 官方映射名
						"finishConfiguration" // 备用名称
				};

				for (String name : possibleNames) {
					try {
						// 尝试查找该名称的方法，且没有参数
						method = clazz.getDeclaredMethod(name);
						BYPASS_LOGGER.info("[BypassCheck] 找到目标方法: {}", name);
						break; // 找到了就跳出循环
					} catch (NoSuchMethodException ignored) {
						// 没找到就继续试下一个名字
					}
				}

				if (method != null) {
					// 暴力赋予访问权限（即使是 private/protected 也能调）
					method.setAccessible(true);
					// 执行方法
					method.invoke(this);
					BYPASS_LOGGER.info("[BypassCheck] 成功跳转至 PLAY 阶段！");
				} else {
					BYPASS_LOGGER.error("[BypassCheck] 致命错误：无法通过反射找到 switchToPlay 或 method_52409 方法！");
				}

			} catch (Exception e) {
				BYPASS_LOGGER.error("[BypassCheck] 反射调用失败: ", e);
			}
		}
	}
}