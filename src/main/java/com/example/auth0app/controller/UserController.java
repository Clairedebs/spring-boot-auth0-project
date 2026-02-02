package com.example.auth0app.controller;

import com.example.auth0app.dto.UserCreateRequest;
import com.example.auth0app.dto.UserResponse;
import com.example.auth0app.dto.UserRolesRequest;
import com.example.auth0app.dto.UserUpdateRequest;
import com.example.auth0app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /api/users - Creating new user with email: {}", request.getEmail());
        UserResponse response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/users - Fetching all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{auth0UserId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String auth0UserId) {
        log.info("GET /api/users/{} - Fetching user by Auth0 ID", auth0UserId);
        UserResponse response = userService.getUserById(auth0UserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{auth0UserId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String auth0UserId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/users/{} - Updating user", auth0UserId);
        UserResponse response = userService.updateUser(auth0UserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        log.info("GET /api/users/me - Fetching current user");
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{auth0UserId}/roles")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable String auth0UserId,
            @Valid @RequestBody UserRolesRequest request) {
        log.info("PATCH /api/users/{}/roles - Updating user roles", auth0UserId);
        UserResponse response = userService.updateUserRoles(auth0UserId, request.getRoles());
        return ResponseEntity.ok(response);
    }
}
