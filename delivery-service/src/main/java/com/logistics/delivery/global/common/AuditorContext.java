package com.logistics.delivery.global.common;

import java.util.Optional;
import java.util.UUID;


public final class AuditorContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private AuditorContext() {
    }

    public static void set(UUID auditorId) {
        CURRENT.set(auditorId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
