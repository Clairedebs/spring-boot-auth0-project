package com.example.auth0app.service;

import com.example.auth0app.dto.UserCreateRequest;
import com.example.auth0app.dto.UserResponse;
import com.example.auth0app.dto.UserUpdateRequest;
import com.example.auth0app.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserService {

    private final Auth0ManagementService auth0ManagementService;

    public UserService(Auth0ManagementService auth0ManagementService) {
        this.auth0ManagementService = auth0ManagementService;
    }

    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        return auth0ManagementService.createUser(request);
    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        // Get first 100 users (can be made configurable)
        return auth0ManagementService.getAllUsers(0, 100);
    }

    public UserResponse getUserById(String auth0UserId) {
        log.info("Fetching user with Auth0 ID: {}", auth0UserId);
        
        UserResponse user = auth0ManagementService.getUserById(auth0UserId);

        // Check authorization - users can only view their own profile or admins can view any
        checkUserAccess(user.getUserId());

        return user;
    }

    public UserResponse updateUser(String auth0UserId, UserUpdateRequest request) {
        log.info("Updating user with Auth0 ID: {}", auth0UserId);

        // Check authorization
        boolean isOwner = isCurrentUser(auth0UserId);
        boolean isAdmin = hasAdminRole();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to update this user");
        }

        // Only admins can update roles
        if (request.getRoles() != null && !isAdmin) {
            throw new UnauthorizedException("Only admins can update user roles");
        }

        return auth0ManagementService.updateUser(auth0UserId, request);
    }

    public UserResponse updateUserRoles(String auth0UserId, Set<String> roles) {
        log.info("Updating roles for user with Auth0 ID: {}", auth0UserId);
        
        // Only admins can update roles (checked by security config)
        return auth0ManagementService.updateUserRoles(auth0UserId, roles);
    }

    public UserResponse getCurrentUser() {
        log.info("Fetching current authenticated user");

        String auth0Id = getCurrentAuth0UserId();
        return auth0ManagementService.getUserById(auth0Id);
    }

    // Helper methods

    private String getCurrentAuth0UserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return authentication.getName();
    }

    private boolean isCurrentUser(String auth0UserId) {
        try {
            String currentAuth0Id = getCurrentAuth0UserId();
            return auth0UserId.equals(currentAuth0Id);
        } catch (UnauthorizedException e) {
            log.debug("No authenticated user found: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error checking if current user: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                         auth.getAuthority().equals("SCOPE_manage:roles"));
    }

    private void checkUserAccess(String auth0UserId) {
        if (!isCurrentUser(auth0UserId) && !hasAdminRole()) {
            throw new UnauthorizedException("You are not authorized to view this user");
        }
    }
}
