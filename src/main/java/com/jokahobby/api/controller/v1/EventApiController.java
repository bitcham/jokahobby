package com.jokahobby.api.controller.v1;

import com.jokahobby.api.dto.request.EventCreateRequest;
import com.jokahobby.api.dto.request.EventUpdateRequest;
import com.jokahobby.api.dto.response.ApiResponse;
import com.jokahobby.api.dto.response.EventListResponse;
import com.jokahobby.api.dto.response.EventResponse;
import com.jokahobby.api.service.EventApplicationService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.CurrentAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventApiController {

    private final EventApplicationService eventApplicationService;

    @PostMapping("/api/v1/hobbies/{path}/events")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @PathVariable String path,
            @CurrentAccount Account account,
            @Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventApplicationService.createEvent(path, account, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/hobbies/{path}/events")
    public ResponseEntity<ApiResponse<List<EventListResponse>>> getEvents(
            @PathVariable String path) {
        List<EventListResponse> response = eventApplicationService.getEvents(path);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/hobbies/{path}/events/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable String path,
            @PathVariable Long eventId,
            @CurrentAccount Account account) {
        EventResponse response = eventApplicationService.getEvent(path, eventId, account);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/api/v1/hobbies/{path}/events/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable String path,
            @PathVariable Long eventId,
            @CurrentAccount Account account,
            @Valid @RequestBody EventUpdateRequest request) {
        EventResponse response = eventApplicationService.updateEvent(path, eventId, account, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/api/v1/hobbies/{path}/events/{eventId}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable String path,
            @PathVariable Long eventId,
            @CurrentAccount Account account) {
        eventApplicationService.deleteEvent(path, eventId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/api/v1/hobbies/{path}/events/{eventId}/enrollments")
    public ResponseEntity<ApiResponse<Void>> enroll(
            @PathVariable String path,
            @PathVariable Long eventId,
            @CurrentAccount Account account) {
        eventApplicationService.enroll(path, eventId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/api/v1/hobbies/{path}/events/{eventId}/enrollments")
    public ResponseEntity<ApiResponse<Void>> disenroll(
            @PathVariable String path,
            @PathVariable Long eventId,
            @CurrentAccount Account account) {
        eventApplicationService.disenroll(path, eventId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptEnrollment(
            @PathVariable String path,
            @PathVariable Long enrollmentId,
            @CurrentAccount Account account) {
        eventApplicationService.acceptEnrollment(path, enrollmentId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectEnrollment(
            @PathVariable String path,
            @PathVariable Long enrollmentId,
            @CurrentAccount Account account) {
        eventApplicationService.rejectEnrollment(path, enrollmentId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/checkin")
    public ResponseEntity<ApiResponse<Void>> checkIn(
            @PathVariable String path,
            @PathVariable Long enrollmentId,
            @CurrentAccount Account account) {
        eventApplicationService.checkIn(path, enrollmentId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/cancel-checkin")
    public ResponseEntity<ApiResponse<Void>> cancelCheckIn(
            @PathVariable String path,
            @PathVariable Long enrollmentId,
            @CurrentAccount Account account) {
        eventApplicationService.cancelCheckIn(path, enrollmentId, account);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
