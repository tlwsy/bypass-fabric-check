package me.tlwsy.bypass.config;

public record BypassSettings(boolean bypassEnabled, boolean filterModdedEffects) {
    public static final BypassSettings DEFAULTS = new BypassSettings(true, false);

    public BypassSettings withBypassEnabled(boolean enabled) {
        return new BypassSettings(enabled, filterModdedEffects);
    }
}
