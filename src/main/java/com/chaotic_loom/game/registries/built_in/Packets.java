package com.chaotic_loom.game.registries.built_in;

import com.chaotic_loom.game.core.Environment;
import com.chaotic_loom.game.core.util.SharedConstants;
import com.chaotic_loom.game.networking.packets.LoginPacket;
import com.chaotic_loom.game.registries.Registry;
import com.chaotic_loom.game.registries.components.Identifier;
import com.chaotic_loom.game.registries.components.Registration;

@Registration(environment = Environment.COMMON)
public class Packets {
    public static LoginPacket LOGIN;

    public static void register() {
        LOGIN = (LoginPacket) Registry.register(RegistryKeys.PACKETS, new Identifier(SharedConstants.NAMESPACE, "login"), new LoginPacket());
    }
}
