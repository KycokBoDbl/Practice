package ru.esie.practice.roomhubb2b.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.esie.practice.roomhubb2b.auth.dto.LoginRequestDto;
import ru.esie.practice.roomhubb2b.auth.dto.ProfileResponseDto;
import ru.esie.practice.roomhubb2b.auth.dto.RegisterRequestDto;
import ru.esie.practice.roomhubb2b.auth.dto.TokenResponseDto;

import java.util.Locale;

@Service
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final String dummyPasswordHash;

    public AuthService(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.dummyPasswordHash = passwordEncoder.encode("roomhub-dummy-credential");
    }

    @Transactional
    public ProfileResponseDto register(RegisterRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (organizationRepository.existsByTaxId(request.taxId())
                || userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new RegistrationConflictException();
        }

        try {
            OrganizationEntity organization = organizationRepository.save(
                    new OrganizationEntity(request.legalName().trim(), request.taxId())
            );
            UserEntity user = userRepository.saveAndFlush(new UserEntity(
                    organization,
                    normalizedEmail,
                    normalizedEmail,
                    passwordEncoder.encode(request.password()),
                    request.role()
            ));
            return toProfile(user);
        } catch (DataIntegrityViolationException exception) {
            throw new RegistrationConflictException();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailNormalized(normalizedEmail).orElse(null);
        String passwordHash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }
        return accessTokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String subject) {
        long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new AccountNotFoundException();
        }
        return userRepository.findById(userId)
                .map(AuthService::toProfile)
                .orElseThrow(AccountNotFoundException::new);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ProfileResponseDto toProfile(UserEntity user) {
        OrganizationEntity organization = user.getOrganization();
        return new ProfileResponseDto(
                user.getId(),
                organization.getId(),
                user.getRole(),
                organization.getLegalName(),
                organization.getTaxId(),
                user.getEmail()
        );
    }
}
