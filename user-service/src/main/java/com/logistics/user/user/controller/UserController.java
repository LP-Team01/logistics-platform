package com.logistics.user.user.controller;

import com.logistics.user.user.dto.request.DeliveryAgentRequestDto;
import com.logistics.user.user.dto.request.UpdateRequestDto;
import com.logistics.user.user.dto.request.UserRequestDto;
import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.dto.response.UserResponseDto;
import com.logistics.user.user.dto.response.UserStatusResponse;
import com.logistics.user.user.dto.response.UserUpdateResponseDto;
import com.logistics.user.user.entity.UserRole;
import com.logistics.user.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "유저", description = "회원가입, 조회, 수정, 승인/반려 등 유저 관리 API")
public class UserController {

    private final UserService userService;


    @Operation(summary = "회원가입", description = "유저 정보를 등록합니다. 등록된 유저는 PENDING 상태로 생성되며 승인이 필요합니다.")
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody @Valid UserRequestDto requestDto){
        UserResponseDto responseDto = userService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(summary = "유저 단건 조회", description = "유저 식별자로 유저 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUser(
        @Parameter(description = "조회할 유저 식별자", required = true) @PathVariable UUID userId
    ){
        UserResponseDto responseDto = userService.getUser(userId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "내 정보 조회", description = "요청자 본인의 유저 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(
        @Parameter(description = "요청자 유저 식별자", required = true) @RequestHeader("X-User-Id") UUID userId
    ){
        UserResponseDto responseDto = userService.getUser(userId);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "허브별 승인 대기 유저 조회", description = "특정 허브에 소속된 PENDING 상태 유저 목록을 조회합니다.")
    @GetMapping("/hubs/{hubId}/pending")
    public ResponseEntity<Page<UserResponseDto>> getPendingUserByHub(
        @Parameter(description = "조회할 허브 식별자", required = true) @PathVariable UUID hubId,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @Parameter(description = "요청자 소속 허브 식별자", required = true) @RequestHeader("X-Hub-Id") UUID requesterHubId
    ){
        Page<UserResponseDto> response = userService.getPendingUserByHub(hubId,requesterHubId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유저 목록 조회·검색", description = "검색 조건에 따라 유저 목록을 페이징하여 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
        @ParameterObject @ModelAttribute UserSearchCondition searchCondition,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<UserResponseDto> response = userService.searchUsers(searchCondition, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유저 정보 수정", description = "유저 정보를 부분 수정합니다.")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserUpdateResponseDto> updateUser(
        @Parameter(description = "수정할 유저 식별자", required = true) @PathVariable UUID userId,
        @RequestBody @Valid UpdateRequestDto requestDto
    ){
        UserUpdateResponseDto responseDto = userService.updateUser(userId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "유저 삭제", description = "유저를 논리 삭제합니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "삭제할 유저 식별자", required = true) @PathVariable UUID userId,
        @Parameter(description = "삭제를 수행한 유저 식별자", required = true) @RequestHeader("X-User-Id") UUID deletedBy
    ){
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "유저 승인", description = "PENDING 상태의 유저를 승인 처리합니다.")
    @PatchMapping("/{userId}/approved")
    public ResponseEntity<UserStatusResponse> approveUser(
        @Parameter(description = "승인할 유저 식별자", required = true) @PathVariable UUID userId,
        @Parameter(description = "승인을 요청한 유저 식별자", required = true) @RequestHeader("X-User-Id") UUID requesterId,
        @Parameter(description = "승인 요청자의 권한", required = true) @RequestHeader("X-User-Role") UserRole managerRole,
        @Parameter(description = "승인 요청자 소속 허브 식별자") @RequestHeader(value = "X-Hub-Id", required = false) UUID managerHubId
    ){
        UserStatusResponse response = userService.approvedUser(userId,requesterId, managerRole, managerHubId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유저 반려", description = "PENDING 상태의 유저를 반려 처리합니다.")
    @PatchMapping("/{userId}/rejected")
    public ResponseEntity<UserStatusResponse> rejectUser(
        @Parameter(description = "반려할 유저 식별자", required = true) @PathVariable UUID userId
    ){
        UserStatusResponse response = userService.rejectUser(userId);
        return ResponseEntity.ok(response);
    }




}
