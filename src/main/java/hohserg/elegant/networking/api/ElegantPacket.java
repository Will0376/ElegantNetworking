package hohserg.elegant.networking.api;

/**
 * Mark annotation for packet classes
 */
public @interface ElegantPacket {
    String channel() default "$modid";
}
