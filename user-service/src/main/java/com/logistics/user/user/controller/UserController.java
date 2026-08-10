package com.logistics.user.user.controller;

import com.logistics.user.user.dto.request.DeliveryAgentRequestDto;
import com.logistics.user.user.dto.request.UpdateRequestDto;
import com.logistics.user.user.dto.request.UserRequestDto;
import com.logistics.user.user.dto.request.UserSearchCondition;
import com.logistics.user.user.dto.response.UserResponseDto;
import com.logistics.user.user.dto.response.UserStatusResponse;
import com.logistics.user.user.dto.response.UserUpdateResponseDto;
import com.logistics.user.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class UserController {

    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody @Valid UserRequestDto requestDto){
        UserResponseDto responseDto = userService.signUp(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable UUID userId){
        UserResponseDto responseDto = userService.getUser(userId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(@RequestHeader("X-User-Id") UUID userId){
        UserResponseDto responseDto = userService.getUser(userId);
        return ResponseEntity.ok(responseDto);
    }
    @GetMapping("/hubs/{hubId}/pending")
    public ResponseEntity<Page<UserResponseDto>> getPendingUserByHub(
        @PathVariable UUID hubId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<UserResponseDto> response = userService.getPendingUserByHub(hubId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
        @ModelAttribute UserSearchCondition searchCondition,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Page<UserResponseDto> response = userService.searchUsers(searchCondition, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserUpdateResponseDto> updateUser(
        @PathVariable UUID userId,
        @RequestBody @Valid UpdateRequestDto requestDto
    ){
        UserUpdateResponseDto responseDto = userService.updateUser(userId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
        @PathVariable UUID userId,
        @RequestHeader("X-User-Id") UUID deletedBy
    ){
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/approved")
    public ResponseEntity<UserStatusResponse> approveUser(
        @PathVariable UUID userId,
        @RequestBody DeliveryAgentRequestDto requestDto
    ){
        UserStatusResponse response = userService.approvedUser(userId, requestDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/rejected")
    public ResponseEntity<UserStatusResponse> rejectUser(
        @PathVariable UUID userId
    ){
        UserStatusResponse response = userService.rejectUser(userId);
        return ResponseEntity.ok(response);
    }





}
