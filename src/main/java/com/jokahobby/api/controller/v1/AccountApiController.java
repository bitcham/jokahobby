package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.*;
import com.jokahobby.api.dto.response.*;
import com.jokahobby.api.service.AccountApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Account")
@RestController
@RequiredArgsConstructor
public class AccountApiController {

    private final AccountApplicationService accountApplicationService;

    @Operation(summary = "Get public profile by nickname")
    @GetMapping("/api/v1/accounts/{nickname}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getPublicProfile(@PathVariable String nickname) {
        Account account = accountApplicationService.getPublicProfile(nickname);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(account)));
    }

    @Operation(summary = "Get my account")
    @GetMapping("/api/v1/accounts/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getMyAccount(@CurrentAccount Account account) {
        return ResponseEntity.ok(ApiResponse.ok(AccountResponse.from(account)));
    }

    @Operation(summary = "Update my profile")
    @PutMapping("/api/v1/accounts/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @CurrentAccount Account account,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Account updated = accountApplicationService.updateProfile(account, request);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(updated)));
    }

    @Operation(summary = "Update notification settings")
    @PutMapping("/api/v1/accounts/me/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotifications(
            @CurrentAccount Account account,
            @RequestBody NotificationUpdateRequest request) {
        accountApplicationService.updateNotifications(account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Update nickname")
    @PutMapping("/api/v1/accounts/me/nickname")
    public ResponseEntity<ApiResponse<AccountResponse>> updateNickname(
            @CurrentAccount Account account,
            @Valid @RequestBody NicknameUpdateRequest request) {
        Account updated = accountApplicationService.updateNickname(account, request.nickname());
        return ResponseEntity.ok(ApiResponse.ok(AccountResponse.from(updated)));
    }

    @Operation(summary = "Get my tags")
    @GetMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(@CurrentAccount Account account) {
        List<Tag> tags = accountApplicationService.getTags(account);
        List<TagResponse> response = tags.stream().map(TagResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Add a tag to my account")
    @PostMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        accountApplicationService.addTag(account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Remove a tag from my account")
    @DeleteMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        accountApplicationService.removeTag(account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Get my zones")
    @GetMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones(@CurrentAccount Account account) {
        List<Zone> zones = accountApplicationService.getZones(account);
        List<ZoneResponse> response = zones.stream().map(ZoneResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Add a zone to my account")
    @PostMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<Void>> addZone(
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        accountApplicationService.addZone(account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Remove a zone from my account")
    @DeleteMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<Void>> removeZone(
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        accountApplicationService.removeZone(account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
