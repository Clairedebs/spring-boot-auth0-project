package com.example.auth0app.service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.TokenRequest;
import com.example.auth0app.dto.UserCreateRequest;
import com.example.auth0app.dto.UserResponse;
import com.example.auth0app.dto.UserUpdateRequest;
import com.example.auth0app.exception.Auth0ApiException;
import com.example.auth0app.exception.UnauthorizedException;
import com.example.auth0app.exception.UserNotFoundException;
import com.example.auth0app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Value("${auth0.domain}")
    private String domain;

    @Value("${auth0.clientId}")
    private String clientId;

    @Value("${auth0.clientSecret}")
    private String clientSecret;

    @Value("${auth0.audience}")
    private String audience;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        // Create user in Auth0
        String auth0UserId;
        try {
            auth0UserId = createAuth0User(request);
        } catch (Auth0Exception e) {
            log.error("Failed to create user in Auth0: {}", e.getMessage());
            throw new Auth0ApiException("Failed to create user in Auth0", e);
        }

        // Create user in local database
        com.example.auth0app.entity.User user = new com.example.auth0app.entity.User();
        user.setAuth0Id(auth0UserId);
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(request.getRoles());

        com.example.auth0app.entity.User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return mapToResponse(savedUser);
    }

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        
        com.example.auth0app.entity.User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check authorization - users can only view their own profile or admins can view any
        checkUserAccess(user);

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user with id: {}", id);

        com.example.auth0app.entity.User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Check authorization
        boolean isOwner = isCurrentUser(user);
        boolean isAdmin = hasAdminRole();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to update this user");
        }

        // Update fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Only admins can update roles
        if (request.getRoles() != null) {
            if (!isAdmin) {
                throw new UnauthorizedException("Only admins can update user roles");
            }
            user.setRoles(request.getRoles());
        }

        // Update in Auth0
        try {
            updateAuth0User(user);
        } catch (Auth0Exception e) {
            log.error("Failed to update user in Auth0: {}", e.getMessage());
            // Continue with local update even if Auth0 fails
        }

        com.example.auth0app.entity.User updatedUser = userRepository.save(user);
        log.info("User updated successfully with id: {}", updatedUser.getId());

        return mapToResponse(updatedUser);
    }

    public UserResponse getCurrentUser() {
        log.info("Fetching current authenticated user");

        String auth0Id = getCurrentAuth0UserId();
        com.example.auth0app.entity.User user = userRepository.findByAuth0Id(auth0Id)
                .orElseThrow(() -> new UserNotFoundException("auth0Id", auth0Id));

        return mapToResponse(user);
    }

    // Helper methods

    private String createAuth0User(UserCreateRequest request) throws Auth0Exception {
        ManagementAPI mgmtApi = getManagementAPI();

        User auth0User = new User();
        auth0User.setEmail(request.getEmail());
        auth0User.setName(request.getFirstName() + " " + request.getLastName());
        auth0User.setGivenName(request.getFirstName());
        auth0User.setFamilyName(request.getLastName());
        auth0User.setConnection("Username-Password-Authentication");
        auth0User.setPassword("TempPassword123!"); // In production, handle this differently

        User createdUser = mgmtApi.users().create(auth0User).execute().getBody();
        
        // Assign roles to the user in Auth0
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            // Note: Role assignment in Auth0 requires additional setup
            // This is a placeholder for the actual implementation
            log.info("Roles to be assigned in Auth0: {}", request.getRoles());
        }

        return createdUser.getId();
    }

    private void updateAuth0User(com.example.auth0app.entity.User user) throws Auth0Exception {
        ManagementAPI mgmtApi = getManagementAPI();

        User auth0User = new User();
        auth0User.setName(user.getFirstName() + " " + user.getLastName());
        auth0User.setGivenName(user.getFirstName());
        auth0User.setFamilyName(user.getLastName());

        mgmtApi.users().update(user.getAuth0Id(), auth0User).execute();
    }

    private ManagementAPI getManagementAPI() throws Auth0Exception {
        // Get Management API token
        AuthAPI authAPI = AuthAPI.newBuilder(domain, clientId, clientSecret).build();
        TokenRequest tokenRequest = authAPI.requestToken(audience);
        String token = tokenRequest.execute().getBody().getAccessToken();

        return ManagementAPI.newBuilder(domain, token).build();
    }

    private String getCurrentAuth0UserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return authentication.getName();
    }

    private boolean isCurrentUser(com.example.auth0app.entity.User user) {
        try {
            String currentAuth0Id = getCurrentAuth0UserId();
            return user.getAuth0Id().equals(currentAuth0Id);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private void checkUserAccess(com.example.auth0app.entity.User user) {
        if (!isCurrentUser(user) && !hasAdminRole()) {
            throw new UnauthorizedException("You are not authorized to view this user");
        }
    }

    private UserResponse mapToResponse(com.example.auth0app.entity.User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoles(user.getRoles());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
