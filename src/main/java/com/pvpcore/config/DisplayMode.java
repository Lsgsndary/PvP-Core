package com.pvpcore.config;

/**
 * How a combat-tagged player sees their combat status.
 */
public enum DisplayMode {
    /** Boss bar countdown (plus chat tag/untag messages). */
    BOSSBAR,
    /** Action bar countdown (plus chat tag/untag messages). */
    ACTIONBAR,
    /** No countdown; only the chat tag/untag messages. */
    CHAT,
    /** Nothing at all: no countdown and no tag/untag messages. */
    HIDDEN;

    public boolean showsTimer() {
        return this == BOSSBAR || this == ACTIONBAR;
    }

    public boolean sendsMessages() {
        return this != HIDDEN;
    }
}
