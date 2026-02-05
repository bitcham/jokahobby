package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.*;
import com.jokahobby.api.dto.response.*;
import com.jokahobby.api.service.AccountApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountApiController {

    private final AccountApplicationService accountApplicationService;

    @GetMapping("/api/v1/accounts/{nickname}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getPublicProfile(@PathVariable String nickname) {
        Account account = accountApplicationService.getPublicProfile(nickname);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(account)));
    }

    @GetMapping("/api/v1/accounts/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getMyAccount(@CurrentAccount Account account) {
        return ResponseEntity.ok(ApiResponse.ok(AccountResponse.from(account)));
    }

    @PutMapping("/api/v1/accounts/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @CurrentAccount Account account,
            @Valid @RequestBody ProfileUpdateRequest request) {
        Account updated = accountApplicationService.updateProfile(account, request);
        return ResponseEntity.ok(ApiResponse.ok(ProfileResponse.from(updated)));
    }

    @PutMapping("/api/v1/accounts/me/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotifications(
            @CurrentAccount Account account,
            @RequestBody NotificationUpdateRequest request) {
        accountApplicationService.updateNotifications(account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/api/v1/accounts/me/nickname")
    public ResponseEntity<ApiResponse<AccountResponse>> updateNickname(
            @CurrentAccount Account account,
            @Valid @RequestBody NicknameUpdateRequest request) {
        Account updated = accountApplicationService.updateNickname(account, request.nickname());
        return ResponseEntity.ok(ApiResponse.ok(AccountResponse.from(updated)));
    }

    @GetMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(@CurrentAccount Account account) {
        List<Tag> tags = accountApplicationService.getTags(account);
        List<TagResponse> response = tags.stream().map(TagResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        accountApplicationService.addTag(account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/accounts/me/tags")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        accountApplicationService.removeTag(account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones(@CurrentAccount Account account) {
        List<Zone> zones = accountApplicationService.getZones(account);
        List<ZoneResponse> response = zones.stream().map(ZoneResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<Void>> addZone(
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        accountApplicationService.addZone(account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/accounts/me/zones")
    public ResponseEntity<ApiResponse<Void>> removeZone(
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        accountApplicationService.removeZone(account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
