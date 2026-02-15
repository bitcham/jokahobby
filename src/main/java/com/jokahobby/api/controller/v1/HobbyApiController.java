package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.HobbyCreateRequest;
import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.HobbyListResponse;
import com.jokahobby.api.dto.response.HobbyMembersResponse;
import com.jokahobby.api.dto.response.HobbyResponse;
import com.jokahobby.api.service.HobbyApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.hobby.HobbySortType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HobbyApiController {

    private final HobbyApplicationService hobbyApplicationService;

    @GetMapping("/api/v1/hobbies")
    public ResponseEntity<ApiResponse<Page<HobbyListResponse>>> getHobbies(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) HobbySortType sortType,
            @PageableDefault(size = 16) Pageable pageable) {
        Page<HobbyListResponse> hobbies = hobbyApplicationService.getPublishedHobbies(country, city, sortType, pageable);
        return ResponseEntity.ok(ApiResponse.ok(hobbies));
    }

    @GetMapping("/api/v1/hobbies/search")
    public ResponseEntity<ApiResponse<Page<HobbyListResponse>>> searchHobbies(
            @RequestParam String keyword,
            @PageableDefault(size = 16) Pageable pageable) {
        Page<HobbyListResponse> hobbies = hobbyApplicationService.searchHobbies(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(hobbies));
    }

    @PostMapping("/api/v1/hobbies")
    public ResponseEntity<ApiResponse<HobbyResponse>> createHobby(
            @CurrentAccount Account account,
            @Valid @RequestBody HobbyCreateRequest request) {
        HobbyResponse response = hobbyApplicationService.createHobby(request, account);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/hobbies/{path}")
    public ResponseEntity<ApiResponse<HobbyResponse>> getHobby(
            @PathVariable String path,
            @CurrentAccount Account account) {
        HobbyResponse response = hobbyApplicationService.getHobbyDetail(path, account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/hobbies/{path}/members")
    public ResponseEntity<ApiResponse<HobbyMembersResponse>> getHobbyMembers(
            @PathVariable String path) {
        HobbyMembersResponse response = hobbyApplicationService.getHobbyMembers(path);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/api/v1/hobbies/{path}/members")
    public ResponseEntity<ApiResponse<Void>> joinHobby(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.joinHobby(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/hobbies/{path}/members")
    public ResponseEntity<ApiResponse<Void>> leaveHobby(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.leaveHobby(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/hobbies/{path}")
    public ResponseEntity<ApiResponse<Void>> deleteHobby(
            @PathVariable String path,
            @CurrentAccount Account account) {
        hobbyApplicationService.deleteHobby(path, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
