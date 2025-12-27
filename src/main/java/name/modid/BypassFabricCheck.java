package name.modid;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BypassFabricCheck implements ModInitializer {
	// 定义一个日志记录器，方便在控制台看到模组已加载
	public static final Logger LOGGER = LoggerFactory.getLogger("bypass-fabric-check");

	@Override
	public void onInitialize() {
		// 因为我们用 Mixin 解决问题，这里留空或者打印一条日志即可
		LOGGER.info("Bypass Fabric Check 模组已加载 - 准备拦截握手验证");
	}
}
