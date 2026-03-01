package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.TokenResponse;
import com.jokahobby.infra.exception.AppException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("dev")
@Tag(name = "Dev", description = "Dev-only endpoints for testing (not available in production)")
@RestController
@RequiredArgsConstructor
public class DevAuthController {

    private final JwtProvider jwtProvider;
    private final AccountRepository accountRepository;

    @Operation(
            summary = "List test accounts",
            description = "Returns all test accounts available for token generation",
            security = {}
    )
    @GetMapping("/api/v1/dev/accounts")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAccounts() {
        List<Map<String, String>> accounts = List.of(
                Map.of("nickname", "alice", "email", "alice@dev.local", "role", "Manager - creates and manages hobbies"),
                Map.of("nickname", "bob", "email", "bob@dev.local", "role", "Member - joins hobbies and enrolls in events"),
                Map.of("nickname", "charlie", "email", "charlie@dev.local", "role", "Observer - clean state, no associations")
        );
        return ResponseEntity.ok(ApiResponse.ok(accounts));
    }

    @Operation(
            summary = "Generate JWT token for test account",
            description = "Returns a JWT access token for the specified test account nickname",
            security = {}
    )
    @PostMapping("/api/v1/dev/token/{nickname}")
    public ResponseEntity<ApiResponse<TokenResponse>> generateToken(@PathVariable String nickname) {
        Account account = accountRepository.findByNickname(nickname)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(account.getId());
        long expiresIn = jwtProvider.getAccessTokenExpirySeconds();

        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(accessToken, expiresIn)));
    }
}
