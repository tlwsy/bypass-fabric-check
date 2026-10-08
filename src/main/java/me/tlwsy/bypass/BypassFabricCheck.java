package me.tlwsy.bypass;

import me.tlwsy.bypass.config.ConfigStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BypassFabricCheck implements ModInitializer {
    public static final String MOD_ID = "bypass-fabric-check";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ConfigStore CONFIG = new ConfigStore(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));

    @Override
    public void onInitialize() {
        try {
            CONFIG.load();
        } catch (IOException error) {
            LOGGER.error("Could not load configuration; using defaults with packet filtering disabled", error);
        }
        LOGGER.info("Bypass Fabric Check - Optimized Edition Initialized.");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("bypass")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .then(Commands.literal("status").executes(ctx -> sendStatus(ctx.getSource())))
                    .then(Commands.literal("on").executes(ctx -> setStatus(ctx.getSource(), true)))
                    .then(Commands.literal("off").executes(ctx -> setStatus(ctx.getSource(), false)))
            );
        });
    }

    private static int sendStatus(CommandSourceStack source) {
        boolean cn = isChinese(source);
        MutableComponent status = CONFIG.get().bypassEnabled()
            ? Component.literal(cn ? "开启" : "Enabled").withStyle(ChatFormatting.GREEN)
            : Component.literal(cn ? "关闭" : "Disabled").withStyle(ChatFormatting.RED);

        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("[Bypass] ").withStyle(ChatFormatting.YELLOW))
            .append(cn ? "当前状态: " : "Current Status: ")
            .append(status), false);
        return 1;
    }

    private static int setStatus(CommandSourceStack source, boolean enable) {
        boolean cn = isChinese(source);
        try {
            CONFIG.setBypassEnabled(enable);
        } catch (IOException error) {
            LOGGER.error("Could not save bypass setting", error);
            source.sendFailure(Component.literal(cn ? "[Bypass] 保存失败，设置未更改。" : "[Bypass] Could not save; settings were not changed."));
            return 0;
        }
        ChatFormatting color = enable ? ChatFormatting.GREEN : ChatFormatting.RED;

        source.sendSuccess(() -> Component.empty()
            .append(Component.literal("[Bypass] ").withStyle(color))
            .append(enable
                ? (cn ? "已开启 (允许原版客户端进入)" : "Enabled (Vanilla clients allowed)")
                : (cn ? "已关闭 (恢复默认检查)" : "Disabled (Default checks restored)")), false);
        return 1;
    }

    private static boolean isChinese(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            String lang = player.clientInformation().language();
            return lang != null && lang.toLowerCase().contains("zh");
        }
        return false;
    }
}
