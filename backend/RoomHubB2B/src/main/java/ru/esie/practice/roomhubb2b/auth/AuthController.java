package ru.esie.practice.roomhubb2b.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.esie.practice.roomhubb2b.auth.dto.LoginRequestDto;
import ru.esie.practice.roomhubb2b.auth.dto.ProfileResponseDto;
import ru.esie.practice.roomhubb2b.auth.dto.RegisterRequestDto;
import ru.esie.practice.roomhubb2b.auth.dto.TokenResponseDto;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Legal entity registration and authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a legal entity account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Legal entity account registered"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email or tax ID is already registered",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ProfileResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token issued"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public TokenResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get the authenticated legal entity profile",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current profile"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing, invalid, expired, or orphaned access token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ProfileResponseDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getProfile(jwt.getSubject());
    }
}
