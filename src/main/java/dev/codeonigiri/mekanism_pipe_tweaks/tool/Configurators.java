package dev.codeonigiri.mekanism_pipe_tweaks.tool;

import mekanism.common.item.ItemConfigurator;
import net.minecraft.world.item.ItemStack;

/**
 * Shared test for "this Configurator is in a mode that configures connections".
 *
 * <p>The guide and the side toggle only replace behaviour that Mekanism's Configurator performs
 * in its configurating modes ({@code CONFIGURATE_ITEMS}/{@code FLUIDS}/{@code CHEMICALS}/
 * {@code ENERGY}/{@code HEAT}). The other modes must keep their vanilla meaning, most importantly
 * {@code WRENCH}, where sneak + right-click dismantles the block.
 */
public final class Configurators {
    private Configurators() {
    }

    public static boolean isCycling(ItemStack stack) {
        return stack.getItem() instanceof ItemConfigurator configurator && configurator.getMode(stack).isConfigurating();
    }
}
