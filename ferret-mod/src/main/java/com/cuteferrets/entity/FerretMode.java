package com.cuteferrets.entity;

import net.minecraft.network.chat.Component;

/**
 * What a tamed ferret is currently doing. Chosen in the ferret menu or with the whistle.
 */
public enum FerretMode {
    /** Follows the owner around. */
    FOLLOW("follow"),
    /** Lies down and stays put. */
    SIT("sit"),
    /** Roams freely around the spot where the mode was set. */
    WANDER("wander"),
    /** Follows the owner and digs up treasure from dirt, sand and gravel. */
    DIG("dig"),
    /** Follows the owner, sniffs out ores and mines the reachable ones. */
    ORE("ore");

    private final String key;

    FerretMode(String key) {
        this.key = key;
    }

    public Component displayName() {
        return Component.translatable("mode.cuteferrets." + key);
    }

    public boolean followsOwner() {
        return this == FOLLOW || this == DIG || this == ORE;
    }

    public static FerretMode byId(int id) {
        FerretMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : FOLLOW;
    }
}
