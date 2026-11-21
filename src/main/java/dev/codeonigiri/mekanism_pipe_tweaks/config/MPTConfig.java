package dev.codeonigiri.mekanism_pipe_tweaks.config;

import mekanism.common.lib.transmitter.TransmissionType;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MPTConfig {
    public enum NonPlayerPlacement {
        VANILLA,
        NONE,
        CLICKED
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_ENERGY = BUILDER
            .comment("Apply the GregTech-style placement rules to Universal Cables.")
            .define("enableEnergy", true);
    public static final ModConfigSpec.BooleanValue ENABLE_FLUID = BUILDER
            .comment("Apply the GregTech-style placement rules to Mechanical Pipes.")
            .define("enableFluid", true);
    public static final ModConfigSpec.BooleanValue ENABLE_CHEMICAL = BUILDER
            .comment("Apply the GregTech-style placement rules to Pressurized Tubes.")
            .define("enableChemical", true);
    public static final ModConfigSpec.BooleanValue ENABLE_HEAT = BUILDER
            .comment("Apply the GregTech-style placement rules to Thermodynamic Conductors.")
            .define("enableHeat", true);
    public static final ModConfigSpec.BooleanValue ENABLE_ITEM = BUILDER
            .comment("Apply the GregTech-style placement rules to Logistical Transporters.")
            .define("enableItem", true);
    public static final ModConfigSpec.BooleanValue CONNECT_CLICKED_SIDE = BUILDER
            .comment("Open the side facing the clicked block. When false every side starts disconnected.")
            .define("connectClickedSide", true);
    public static final ModConfigSpec.BooleanValue OPEN_COUNTERPART_SIDE = BUILDER
            .comment("When placing against another transmitter, also open that transmitter's facing side.")
            .define("openCounterpartSide", true);
    public static final ModConfigSpec.EnumValue<NonPlayerPlacement> NON_PLAYER_PLACEMENT = BUILDER
            .comment("Placement by dispensers, structures and other non-player sources.",
                    "VANILLA: keep Mekanism's default of every side connected.",
                    "NONE: disconnect every side.",
                    "CLICKED: open the side facing the block that was placed against.")
            .defineEnum("nonPlayerPlacement", NonPlayerPlacement.VANILLA);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private MPTConfig() {
    }

    /**
     * Per-transmission-type gate.
     *
     * <p>Any transmitter - vanilla or from another Mekanism addon - exposes the standard
     * {@link TransmissionType} values through
     * {@link mekanism.common.content.network.transmitter.Transmitter#getSupportedTransmissionTypes()},
     * so this switch covers every transmitter that can exist. A transmitter is governed by this mod
     * when any of the types it supports is enabled here; only a transmitter whose supported types
     * are <em>all</em> disabled is left untouched. That multi-type rule lives in
     * {@code Transmitters}; this method is intentionally the single type-to-flag mapping.
     */
    public static boolean isEnabled(TransmissionType type) {
        return switch (type) {
            case ENERGY -> ENABLE_ENERGY.get();
            case FLUID -> ENABLE_FLUID.get();
            case CHEMICAL -> ENABLE_CHEMICAL.get();
            case HEAT -> ENABLE_HEAT.get();
            case ITEM -> ENABLE_ITEM.get();
        };
    }
}
