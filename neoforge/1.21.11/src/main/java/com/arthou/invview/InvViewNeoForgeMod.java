package com.arthou.invview;

import com.arthou.invview.command.InvViewCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(InvViewNeoForgeMod.MOD_ID)
public final class InvViewNeoForgeMod {
    public static final String MOD_ID = "invview";

    public InvViewNeoForgeMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(InvViewCommands::register);
    }
}
