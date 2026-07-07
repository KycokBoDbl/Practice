package ru.esie.practice.roomhubb2b.listing;

import org.springframework.security.oauth2.jwt.Jwt;
import ru.esie.practice.roomhubb2b.auth.UserRole;

public record ListingActor(Long organizationId, UserRole role) {

    public static ListingActor from(Jwt jwt) {
        try {
            Number organizationId = jwt.getClaim("organizationId");
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            if (organizationId == null) {
                throw new IllegalArgumentException("organizationId is missing");
            }
            return new ListingActor(organizationId.longValue(), role);
        } catch (RuntimeException exception) {
            throw new ListingForbiddenException("Access token does not contain a valid listing actor");
        }
    }
}
