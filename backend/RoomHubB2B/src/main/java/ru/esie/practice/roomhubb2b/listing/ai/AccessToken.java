package ru.esie.practice.roomhubb2b.listing.ai;

import java.time.Duration;
import java.time.Instant;

record AccessToken(String value, Instant expiresAt) {

    boolean isUsable(Instant now, Duration safetySkew) {
        return now.plus(safetySkew).isBefore(expiresAt);
    }
}
