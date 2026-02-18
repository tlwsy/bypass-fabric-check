package me.tlwsy.bypass;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BypassFabricCheck implements ModInitializer {
    public static final String MOD_ID = "bypass-fabric-check";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean IS_ENABLED = true;

    @Override
    public void onInitialize() {
        LOGGER.info("Bypass Fabric Check - Optimized Edition Initialized.");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("bypass")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.literal("status").executes(ctx -> sendStatus(ctx.getSource())))
                    .then(CommandManager.literal("on").executes(ctx -> setStatus(ctx.getSource(), true)))
                    .then(CommandManager.literal("off").executes(ctx -> setStatus(ctx.getSource(), false)))
            );
        });
    }

    private static int sendStatus(ServerCommandSource source) {
        boolean cn = isChinese(source);
        MutableText status = IS_ENABLED 
            ? Text.literal(cn ? "开启" : "Enabled").formatted(Formatting.GREEN)
            : Text.literal(cn ? "关闭" : "Disabled").formatted(Formatting.RED);
        
        source.sendMessage(Text.empty()
            .append(Text.literal("[Bypass] ").formatted(Formatting.YELLOW))
            .append(cn ? "当前状态: " : "Current Status: ")
            .append(status));
        return 1;
    }

    private static int setStatus(ServerCommandSource source, boolean enable) {
        IS_ENABLED = enable;
        boolean cn = isChinese(source);
        Formatting color = enable ? Formatting.GREEN : Formatting.RED;
        
        MutableText msg = Text.empty()
            .append(Text.literal("[Bypass] ").formatted(color))
            .append(enable 
                ? (cn ? "已开启 (允许原版客户端进入)" : "Enabled (Vanilla clients allowed)")
                : (cn ? "已关闭 (恢复默认检查)" : "Disabled (Default checks restored)"));
        
        source.sendMessage(msg);
        return 1;
    }

    private static boolean isChinese(ServerCommandSource source) {
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            var options = player.getClientOptions();
            return options != null && options.language() != null && options.language().toLowerCase().contains("zh");
        }
        return false;
    }
}