package com.logistics.user.controller;

import com.logistics.user.dto.request.UserRequestDto;
import com.logistics.user.dto.response.UserResponseDto;
import com.logistics.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private UserService userService;
}
