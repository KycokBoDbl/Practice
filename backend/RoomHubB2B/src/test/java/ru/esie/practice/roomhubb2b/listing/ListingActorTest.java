package ru.esie.practice.roomhubb2b.listing;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.esie.practice.roomhubb2b.auth.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingActorTest {

    @Test
    void readsOrganizationAndRoleFromJwt() {
        ListingActor actor = ListingActor.from(jwt(17L, "LANDLORD"));

        assertThat(actor).isEqualTo(new ListingActor(17L, UserRole.LANDLORD));
    }

    @Test
    void rejectsMissingOrganizationAndUnknownRole() {
        assertThatThrownBy(() -> ListingActor.from(jwt(null, "LANDLORD")))
                .isInstanceOf(ListingForbiddenException.class);
        assertThatThrownBy(() -> ListingActor.from(jwt(17L, "ADMIN")))
                .isInstanceOf(ListingForbiddenException.class);
    }

    private Jwt jwt(Long organizationId, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", role);
        if (organizationId != null) {
            builder.claim("organizationId", organizationId);
        }
        return builder.build();
    }
}
