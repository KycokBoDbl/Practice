package ru.esie.practice.roomhubb2b.booking;

import org.springframework.security.oauth2.jwt.Jwt;
import ru.esie.practice.roomhubb2b.auth.UserRole;

public record BookingActor(Long organizationId, UserRole role) {

    public static BookingActor from(Jwt jwt) {
        try {
            Number organizationId = jwt.getClaim("organizationId");
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            if (organizationId == null) {
                throw new IllegalArgumentException("organizationId is missing");
            }
            return new BookingActor(organizationId.longValue(), role);
        } catch (RuntimeException exception) {
            throw new BookingForbiddenException("Access token does not contain a valid booking actor");
        }
    }
}
