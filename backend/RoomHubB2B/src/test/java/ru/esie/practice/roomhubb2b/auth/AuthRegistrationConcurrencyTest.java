package ru.esie.practice.roomhubb2b.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.esie.practice.roomhubb2b.auth.dto.ProfileResponseDto;
import ru.esie.practice.roomhubb2b.auth.dto.RegisterRequestDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthRegistrationConcurrencyTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void keepsExactlyOneAccountWhenRegistrationsRace() {
        String taxId = "7600000099";
        String email = "registration-race@example.com";
        RegisterRequestDto request = new RegisterRequestDto(
                UserRole.TENANT,
                "ООО Registration Race",
                taxId,
                email,
                "S3cure-roomhub-password"
        );
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<CompletableFuture<Object>> attempts = List.of(
                    attemptRegistration(executor, start, request),
                    attemptRegistration(executor, start, request)
            );
            start.countDown();
            List<Object> results = attempts.stream().map(CompletableFuture::join).toList();

            assertThat(results).filteredOn(ProfileResponseDto.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(RegistrationConflictException.class::isInstance).hasSize(1);
            assertThat(organizationRepository.findByTaxId(taxId)).isPresent();
            assertThat(userRepository.findByEmailNormalized(email)).isPresent();
        } finally {
            executor.shutdownNow();
            userRepository.findByEmailNormalized(email).ifPresent(userRepository::delete);
            organizationRepository.findByTaxId(taxId).ifPresent(organizationRepository::delete);
        }
    }

    private CompletableFuture<Object> attemptRegistration(
            ExecutorService executor,
            CountDownLatch start,
            RegisterRequestDto request
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                start.await();
                return authService.register(request);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } catch (RegistrationConflictException exception) {
                return exception;
            }
        }, executor);
    }
}
