package com.arthou.invview;

import com.arthou.invview.command.InvViewCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(InvViewForgeMod.MOD_ID)
public final class InvViewForgeMod {
    public static final String MOD_ID = "invview";

    public InvViewForgeMod() {
        MinecraftForge.EVENT_BUS.addListener(InvViewCommands::register);
    }
}
