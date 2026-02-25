package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.*;
import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.HobbySettingsResponse;
import com.jokahobby.api.dto.response.TagResponse;
import com.jokahobby.api.dto.response.ZoneResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.jokahobby.api.service.HobbyApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Hobby Settings")
@RestController
@RequiredArgsConstructor
public class HobbySettingsApiController {

    private final HobbyApplicationService hobbyApplicationService;

    @Operation(summary = "Get hobby settings")
    @GetMapping("/api/v1/hobbies/{path}/settings")
    public ResponseEntity<ApiResponse<HobbySettingsResponse>> getSettings(
            @PathVariable String path,
            @CurrentAccount Account account) {
        HobbySettingsResponse response = hobbyApplicationService.getHobbySettings(path, account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Update hobby description")
    @PutMapping("/api/v1/hobbies/{path}/settings/description")
    public ResponseEntity<ApiResponse<Void>> updateDescription(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody HobbyDescriptionUpdateRequest request) {
        hobbyApplicationService.updateDescription(path, account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Update hobby banner image")
    @PutMapping("/api/v1/hobbies/{path}/settings/banner")
    public ResponseEntity<ApiResponse<Void>> updateBanner(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody HobbyBannerUpdateRequest request) {
        hobbyApplicationService.updateBanner(path, account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Enable hobby banner")
    @PostMapping("/api/v1/hobbies/{path}/settings/banner/enable")
    public ResponseEntity<ApiResponse<Void>> enableBanner(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.enableBanner(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Disable hobby banner")
    @PostMapping("/api/v1/hobbies/{path}/settings/banner/disable")
    public ResponseEntity<ApiResponse<Void>> disableBanner(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.disableBanner(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Get hobby tags")
    @GetMapping("/api/v1/hobbies/{path}/settings/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(
            @PathVariable String path,
            @CurrentAccount Account account) {
        List<TagResponse> response = hobbyApplicationService.getHobbyTags(path, account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Add a tag to hobby")
    @PostMapping("/api/v1/hobbies/{path}/settings/tags")
    public ResponseEntity<ApiResponse<Void>> addTag(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        hobbyApplicationService.addHobbyTag(path, account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Remove a tag from hobby")
    @DeleteMapping("/api/v1/hobbies/{path}/settings/tags")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody TagRequest request) {
        hobbyApplicationService.removeHobbyTag(path, account, request.tagTitle());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Get hobby zones")
    @GetMapping("/api/v1/hobbies/{path}/settings/zones")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones(
            @PathVariable String path,
            @CurrentAccount Account account) {
        List<ZoneResponse> response = hobbyApplicationService.getHobbyZones(path, account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Add a zone to hobby")
    @PostMapping("/api/v1/hobbies/{path}/settings/zones")
    public ResponseEntity<ApiResponse<Void>> addZone(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        hobbyApplicationService.addHobbyZone(path, account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Remove a zone from hobby")
    @DeleteMapping("/api/v1/hobbies/{path}/settings/zones")
    public ResponseEntity<ApiResponse<Void>> removeZone(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody ZoneRequest request) {
        hobbyApplicationService.removeHobbyZone(path, account, request.zoneName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Publish a hobby")
    @PostMapping("/api/v1/hobbies/{path}/settings/publish")
    public ResponseEntity<ApiResponse<Void>> publish(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.publish(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Close a hobby")
    @PostMapping("/api/v1/hobbies/{path}/settings/close")
    public ResponseEntity<ApiResponse<Void>> close(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.close(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Start recruiting members")
    @PostMapping("/api/v1/hobbies/{path}/settings/recruit/start")
    public ResponseEntity<ApiResponse<Void>> startRecruit(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.startRecruit(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Stop recruiting members")
    @PostMapping("/api/v1/hobbies/{path}/settings/recruit/stop")
    public ResponseEntity<ApiResponse<Void>> stopRecruit(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.stopRecruit(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Promote member to manager")
    @PostMapping("/api/v1/hobbies/{path}/settings/managers")
    public ResponseEntity<ApiResponse<Void>> promoteToManager(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody ManagerPromoteRequest request) {
        hobbyApplicationService.promoteToManager(path, account, request.nickname());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Demote manager to member")
    @DeleteMapping("/api/v1/hobbies/{path}/settings/managers/{nickname}")
    public ResponseEntity<ApiResponse<Void>> demoteToMember(
            @PathVariable String path,
            @PathVariable String nickname,
            @CurrentAccount Account account) {
        hobbyApplicationService.demoteToMember(path, account, nickname);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Transfer host role")
    @PostMapping("/api/v1/hobbies/{path}/settings/host")
    public ResponseEntity<ApiResponse<Void>> transferHost(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody HostTransferRequest request) {
        hobbyApplicationService.transferHost(path, account, request.nickname());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Update hobby URL path")
    @PutMapping("/api/v1/hobbies/{path}/settings/path")
    public ResponseEntity<ApiResponse<Void>> updatePath(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody HobbyPathUpdateRequest request) {
        hobbyApplicationService.updatePath(path, account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Update hobby title")
    @PutMapping("/api/v1/hobbies/{path}/settings/title")
    public ResponseEntity<ApiResponse<Void>> updateTitle(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody HobbyTitleUpdateRequest request) {
        hobbyApplicationService.updateTitle(path, account, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
