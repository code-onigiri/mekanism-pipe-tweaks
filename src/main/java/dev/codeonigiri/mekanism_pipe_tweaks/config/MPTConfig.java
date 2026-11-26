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
            .comment("When placing while sneaking, open the side facing the clicked block (and, against a",
                    "transmitter, that transmitter's counterpart side). When false every side starts",
                    "disconnected even when sneaking; without sneaking nothing is opened either way.")
            .define("connectClickedSide", true);
    public static final ModConfigSpec.BooleanValue CONNECT_PLACED_PIPE = BUILDER
            .comment("When a transmitter is placed against a same-genre transmitter, connect the two",
                    "facing sides regardless of sneaking (GregTech's pipe placement behaviour).")
            .define("connectPlacedPipe", true);
    public static final ModConfigSpec.BooleanValue OPEN_COUNTERPART_SIDE = BUILDER
            .comment("When connecting a side (on placement or with the Configurator), also open a",
                    "compatible neighbour transmitter's facing side. Mekanism needs both sides open",
                    "before the pair actually connects.")
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
