package me.tlwsy.bypass.client;

import me.tlwsy.bypass.BypassFabricCheck;
import me.tlwsy.bypass.config.BypassSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class BypassConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 30;
    private static final int FOOTER_HEIGHT = 34;
    private static final int LIST_TOP = 72;
    private static final int ROW_HEIGHT = 32;

    private final Screen parent;
    private final BypassSettings initialSettings;
    private boolean bypassEnabled;
    private boolean filterModdedEffects;
    private boolean saveFailed;
    private String query = "";
    private SettingsList settingsList;
    private List<SettingEntry> entries;
    private Button saveButton;

    public BypassConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        initialSettings = BypassFabricCheck.CONFIG.get();
        bypassEnabled = initialSettings.bypassEnabled();
        filterModdedEffects = initialSettings.filterModdedEffects();
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(720, width - 2 * Math.max(16, width / 12));
        int left = (width - contentWidth) / 2;
        EditBox search = new EditBox(font, left + 4, HEADER_HEIGHT + 12, contentWidth - 8, 20, text("search"));
        search.setHint(text("search"));
        search.setMaxLength(128);
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            filterEntries();
        });
        addRenderableWidget(search);

        settingsList = addRenderableWidget(new SettingsList(contentWidth));
        entries = List.of(
                new SettingEntry("bypass", bypassEnabled, BypassSettings.DEFAULTS.bypassEnabled(), value -> bypassEnabled = value),
                new SettingEntry("effects", filterModdedEffects, BypassSettings.DEFAULTS.filterModdedEffects(), value -> filterModdedEffects = value));
        filterEntries();

        int buttonWidth = Math.min(150, (width - 40) / 2);
        int footerY = height - FOOTER_HEIGHT + 7;
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(width / 2 - buttonWidth - 4, footerY, buttonWidth, 20).build());
        saveButton = addRenderableWidget(Button.builder(text("save"), button -> save())
                .bounds(width / 2 + 4, footerY, buttonWidth, 20).build());
        updateSaveButton();
    }

    private void filterEntries() {
        String term = query.strip().toLowerCase(Locale.ROOT);
        settingsList.replaceEntries(entries.stream().filter(entry -> entry.matches(term)).toList());
        settingsList.setScrollAmount(0);
    }

    private void updateSaveButton() {
        saveButton.active = !new BypassSettings(bypassEnabled, filterModdedEffects).equals(initialSettings);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (minecraft.level == null) extractPanorama(graphics, delta);
        graphics.fill(0, 0, width, height, 0x35000000);
        graphics.fill(0, HEADER_HEIGHT, width, height - FOOTER_HEIGHT, 0x65000000);
        graphics.horizontalLine(0, width, HEADER_HEIGHT, 0xFF808080);
        graphics.horizontalLine(0, width, height - FOOTER_HEIGHT, 0xFF808080);
        minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        if (mouseY >= 8 && mouseY < 25 && Math.abs(mouseX - width / 2) < font.width(title) / 2) {
            graphics.setTooltipForNextFrame(text("scope"), mouseX, mouseY);
        }
        if (settingsList.children().isEmpty()) {
            graphics.centeredText(font, text("no_results"), width / 2, LIST_TOP + 12, 0xFFAAAAAA);
        }
        if (saveFailed) {
            graphics.centeredText(font, text("save_failed"), width / 2, height - FOOTER_HEIGHT - 12, 0xFFFF5555);
        }
    }

    @Override
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        // Search and draft values survive resizing; recalculate the two-column layout.
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private void save() {
        try {
            BypassFabricCheck.CONFIG.save(new BypassSettings(bypassEnabled, filterModdedEffects));
            onClose();
        } catch (IOException error) {
            BypassFabricCheck.LOGGER.error("Could not save configuration", error);
            saveFailed = true;
        }
    }

    private static MutableComponent text(String key) {
        return Component.translatable("config.bypass-fabric-check." + key);
    }

    private final class SettingsList extends ContainerObjectSelectionList<SettingEntry> {
        private final int rowWidth;

        private SettingsList(int rowWidth) {
            super(BypassConfigScreen.this.minecraft, BypassConfigScreen.this.width,
                    Math.max(32, BypassConfigScreen.this.height - LIST_TOP - FOOTER_HEIGHT - 14), LIST_TOP, ROW_HEIGHT);
            this.rowWidth = rowWidth;
            centerListVertically = false;
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        @Override
        protected void extractListBackground(GuiGraphicsExtractor graphics) {
            // The screen supplies one continuous translucent panel.
        }

        @Override
        protected void extractListSeparators(GuiGraphicsExtractor graphics) {
        }
    }

    private final class SettingEntry extends ContainerObjectSelectionList.Entry<SettingEntry> {
        private final Component label;
        private final Component description;
        private final boolean defaultValue;
        private final Consumer<Boolean> onChange;
        private final CycleButton<Boolean> toggle;
        private final Button reset;

        private SettingEntry(String key, boolean value, boolean defaultValue, Consumer<Boolean> onChange) {
            label = text(key);
            description = text(key + ".tooltip");
            this.defaultValue = defaultValue;
            this.onChange = onChange;
            toggle = CycleButton.onOffBuilder(value).displayOnlyValue()
                    .withTooltip(ignored -> Tooltip.create(description))
                    .create(0, 0, 120, 20, label, (button, updated) -> changed(updated));
            reset = Button.builder(text("reset"), button -> {
                toggle.setValue(defaultValue);
                changed(defaultValue);
            }).size(40, 20).tooltip(Tooltip.create(Component.translatable("config.bypass-fabric-check.reset.tooltip", label))).build();
            reset.active = value != defaultValue;
        }

        private void changed(boolean value) {
            onChange.accept(value);
            reset.active = value != defaultValue;
            updateSaveButton();
        }

        private boolean matches(String term) {
            return label.getString().toLowerCase(Locale.ROOT).contains(term)
                    || description.getString().toLowerCase(Locale.ROOT).contains(term);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int toggleWidth = Math.min(165, getContentWidth() / 3);
            int right = getContentRight();
            int controlY = getContentYMiddle() - 10;
            reset.setPosition(right - 40, controlY);
            toggle.setWidth(toggleWidth);
            toggle.setPosition(reset.getX() - toggleWidth - 4, controlY);

            int labelWidth = Math.max(40, toggle.getX() - getContentX() - 12);
            var lines = font.split(label, labelWidth);
            int textY = getContentYMiddle() - Math.min(2, lines.size()) * font.lineHeight / 2;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.text(font, lines.get(i), getContentX(), textY + i * font.lineHeight, 0xFFCCCCCC);
            }
            toggle.extractRenderState(graphics, mouseX, mouseY, delta);
            reset.extractRenderState(graphics, mouseX, mouseY, delta);
            if (hovered && mouseX < toggle.getX()) {
                graphics.setTooltipForNextFrame(description, mouseX, mouseY);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggle, reset);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(toggle, reset);
        }
    }
}
