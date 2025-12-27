package name.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BypassFabricCheck implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("bypass-fabric-check");

	// 全局开关
	public static boolean IS_ENABLED = true;

	@Override
	public void onInitialize() {
		LOGGER.info("Bypass Fabric Check 模组已加载");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("bypass")
					// 1. 查看状态
					.then(CommandManager.literal("status")
							.executes(context -> {
								boolean isCn = isChinese(context.getSource());

								String statusFn = IS_ENABLED
										? (isCn ? "§a开启" : "§aEnabled")
										: (isCn ? "§c关闭" : "§cDisabled");

								String msg = isCn
										? "§e[BypassCheck] 当前状态: " + statusFn
										: "§e[BypassCheck] Current Status: " + statusFn;

								context.getSource().sendMessage(Text.of(msg));
								return 1;
							})
					)
					// 2. 开启
					.then(CommandManager.literal("on")
							.executes(context -> {
								IS_ENABLED = true;
								boolean isCn = isChinese(context.getSource());
								String msg = isCn
										? "§a[BypassCheck] 已开启绕过验证！(Vanilla 客户端可进入)"
										: "§a[BypassCheck] Bypass Enabled! (Vanilla clients allowed)";
								context.getSource().sendMessage(Text.of(msg));
								return 1;
							})
					)
					// 3. 关闭
					.then(CommandManager.literal("off")
							.executes(context -> {
								IS_ENABLED = false;
								boolean isCn = isChinese(context.getSource());
								String msg = isCn
										? "§c[BypassCheck] 已关闭绕过验证！(恢复安全模式)"
										: "§c[BypassCheck] Bypass Disabled! (Secure mode restored)";
								context.getSource().sendMessage(Text.of(msg));
								return 1;
							})
					)
			);
		});
	}

	/**
	 * 自动检测指令发送者是否使用中文
	 */
	private boolean isChinese(ServerCommandSource source) {
		try {
			// 如果是玩家发送的
			if (source.getEntity() instanceof ServerPlayerEntity player) {
				// 获取玩家的语言设置 (例如 "zh_cn", "en_us")
				// 注意：clientOptions() 是 1.21+ 的新 API
				String lang = player.getClientOptions().language();
				return lang.toLowerCase().contains("zh");
			}
		} catch (Exception e) {
			// 如果获取失败（比如版本差异），默认当作英文处理，或者看日志
		}
		// 如果是控制台 (Console) 或者获取失败，默认返回 false (英文)
		// 如果您希望控制台默认是中文，把这里改成 true
		return false;
	}
}