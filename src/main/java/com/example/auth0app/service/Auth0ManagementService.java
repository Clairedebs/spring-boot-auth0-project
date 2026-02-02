package com.example.auth0app.service;

import com.auth0.client.auth.AuthAPI;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.UserFilter;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.net.TokenRequest;
import com.example.auth0app.config.Auth0Properties;
import com.example.auth0app.dto.UserCreateRequest;
import com.example.auth0app.dto.UserResponse;
import com.example.auth0app.dto.UserUpdateRequest;
import com.example.auth0app.exception.Auth0ApiException;
import com.example.auth0app.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Auth0ManagementService {

    private final Auth0Properties auth0Properties;
    
    private volatile String cachedManagementToken;
    private volatile long tokenExpiryTime;
    private final Object tokenLock = new Object();

    public Auth0ManagementService(Auth0Properties auth0Properties) {
        this.auth0Properties = auth0Properties;
    }

    /**
     * Create a new user in Auth0
     */
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user in Auth0 with email: {}", request.getEmail());
        
        try {
            ManagementAPI mgmtApi = getManagementAPI();

            User auth0User = new User();
            auth0User.setEmail(request.getEmail());
            auth0User.setPassword(request.getPassword().toCharArray());
            auth0User.setName(request.getFirstName() + " " + request.getLastName());
            auth0User.setGivenName(request.getFirstName());
            auth0User.setFamilyName(request.getLastName());
            auth0User.setConnection(auth0Properties.getConnection());
            
            // Set email verified to false - user will receive verification email
            auth0User.setEmailVerified(false);
            auth0User.setVerifyEmail(true);

            // Add user metadata for roles
            Map<String, Object> userMetadata = new HashMap<>();
            userMetadata.put("roles", request.getRoles());
            auth0User.setUserMetadata(userMetadata);

            User createdUser = mgmtApi.users().create(auth0User).execute().getBody();
            log.info("User created successfully in Auth0 with ID: {}", createdUser.getId());
            
            return mapToUserResponse(createdUser, request.getRoles());
            
        } catch (Auth0Exception e) {
            log.error("Failed to create user in Auth0: {}", e.getMessage());
            throw new Auth0ApiException("Failed to create user in Auth0: " + e.getMessage(), e);
        }
    }

    /**
     * Get all users from Auth0 with pagination
     */
    public List<UserResponse> getAllUsers(int page, int perPage) {
        log.info("Fetching users from Auth0 - page: {}, perPage: {}", page, perPage);
        
        try {
            ManagementAPI mgmtApi = getManagementAPI();
            
            UserFilter filter = new UserFilter()
                    .withPage(page, perPage);
            
            List<User> users = mgmtApi.users().list(filter).execute().getBody().getItems();
            
            return users.stream()
                    .map(user -> mapToUserResponse(user, extractRolesFromUser(user)))
                    .collect(Collectors.toList());
                    
        } catch (Auth0Exception e) {
            log.error("Failed to fetch users from Auth0: {}", e.getMessage());
            throw new Auth0ApiException("Failed to fetch users from Auth0: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by Auth0 user ID
     */
    public UserResponse getUserById(String auth0UserId) {
        log.info("Fetching user from Auth0 with ID: {}", auth0UserId);
        
        try {
            ManagementAPI mgmtApi = getManagementAPI();
            User user = mgmtApi.users().get(auth0UserId, null).execute().getBody();
            
            if (user == null) {
                throw new UserNotFoundException("auth0UserId", auth0UserId);
            }
            
            return mapToUserResponse(user, extractRolesFromUser(user));
            
        } catch (Auth0Exception e) {
            log.error("Failed to fetch user from Auth0: {}", e.getMessage());
            // Check if user not found based on error message or code
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("not found"))) {
                throw new UserNotFoundException("auth0UserId", auth0UserId);
            }
            throw new Auth0ApiException("Failed to fetch user from Auth0: " + e.getMessage(), e);
        }
    }

    /**
     * Update user in Auth0
     */
    public UserResponse updateUser(String auth0UserId, UserUpdateRequest request) {
        log.info("Updating user in Auth0 with ID: {}", auth0UserId);
        
        try {
            ManagementAPI mgmtApi = getManagementAPI();

            User auth0User = new User();
            auth0User.setName(request.getFirstName() + " " + request.getLastName());
            auth0User.setGivenName(request.getFirstName());
            auth0User.setFamilyName(request.getLastName());

            // Update user metadata for roles if provided
            if (request.getRoles() != null) {
                Map<String, Object> userMetadata = new HashMap<>();
                userMetadata.put("roles", request.getRoles());
                auth0User.setUserMetadata(userMetadata);
            }

            User updatedUser = mgmtApi.users().update(auth0UserId, auth0User).execute().getBody();
            log.info("User updated successfully in Auth0 with ID: {}", updatedUser.getId());
            
            Set<String> roles = request.getRoles() != null ? request.getRoles() : extractRolesFromUser(updatedUser);
            return mapToUserResponse(updatedUser, roles);
            
        } catch (Auth0Exception e) {
            log.error("Failed to update user in Auth0: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("not found"))) {
                throw new UserNotFoundException("auth0UserId", auth0UserId);
            }
            throw new Auth0ApiException("Failed to update user in Auth0: " + e.getMessage(), e);
        }
    }

    /**
     * Update user roles in Auth0
     */
    public UserResponse updateUserRoles(String auth0UserId, Set<String> roles) {
        log.info("Updating roles for user in Auth0 with ID: {}", auth0UserId);
        
        try {
            ManagementAPI mgmtApi = getManagementAPI();

            User auth0User = new User();
            Map<String, Object> userMetadata = new HashMap<>();
            userMetadata.put("roles", roles);
            auth0User.setUserMetadata(userMetadata);

            User updatedUser = mgmtApi.users().update(auth0UserId, auth0User).execute().getBody();
            log.info("User roles updated successfully in Auth0 with ID: {}", updatedUser.getId());
            
            return mapToUserResponse(updatedUser, roles);
            
        } catch (Auth0Exception e) {
            log.error("Failed to update user roles in Auth0: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("not found"))) {
                throw new UserNotFoundException("auth0UserId", auth0UserId);
            }
            throw new Auth0ApiException("Failed to update user roles in Auth0: " + e.getMessage(), e);
        }
    }

    /**
     * Get Management API access token (thread-safe)
     */
    private ManagementAPI getManagementAPI() throws Auth0Exception {
        // Check if we have a cached token that's still valid (outside lock for performance)
        long currentTime = System.currentTimeMillis();
        if (cachedManagementToken != null && currentTime < tokenExpiryTime) {
            return ManagementAPI.newBuilder(auth0Properties.getDomain(), cachedManagementToken).build();
        }
        
        // Double-check locking pattern for thread-safe token refresh
        synchronized (tokenLock) {
            // Re-check after acquiring lock
            currentTime = System.currentTimeMillis();
            if (cachedManagementToken != null && currentTime < tokenExpiryTime) {
                return ManagementAPI.newBuilder(auth0Properties.getDomain(), cachedManagementToken).build();
            }
            
            log.debug("Getting new Management API token");
            
            // Get Management API token
            AuthAPI authAPI = AuthAPI.newBuilder(
                auth0Properties.getDomain(), 
                auth0Properties.getManagement().getClientId(), 
                auth0Properties.getManagement().getClientSecret()
            ).build();
            
            // Request token for Management API
            String managementAudience = "https://" + auth0Properties.getDomain() + "/api/v2/";
            TokenRequest tokenRequest = authAPI.requestToken(managementAudience);
            com.auth0.json.auth.TokenHolder tokenHolder = tokenRequest.execute().getBody();
            
            cachedManagementToken = tokenHolder.getAccessToken();
            // Set expiry to 23 hours (tokens typically expire in 24 hours)
            tokenExpiryTime = currentTime + (23 * 60 * 60 * 1000);

            log.debug("Management API token obtained successfully");
            return ManagementAPI.newBuilder(auth0Properties.getDomain(), cachedManagementToken).build();
        }
    }

    /**
     * Map Auth0 User to UserResponse
     */
    private UserResponse mapToUserResponse(User user, Set<String> roles) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getGivenName() != null ? user.getGivenName() : "");
        response.setLastName(user.getFamilyName() != null ? user.getFamilyName() : "");
        response.setRoles(roles != null ? roles : new HashSet<>());
        response.setEmailVerified(user.isEmailVerified() != null ? user.isEmailVerified() : false);
        
        // Parse dates
        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().toInstant());
        }
        if (user.getUpdatedAt() != null) {
            response.setUpdatedAt(user.getUpdatedAt().toInstant());
        }
        
        return response;
    }

    /**
     * Extract roles from Auth0 user metadata
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractRolesFromUser(User user) {
        if (user.getUserMetadata() != null && user.getUserMetadata().containsKey("roles")) {
            Object rolesObj = user.getUserMetadata().get("roles");
            if (rolesObj instanceof Collection) {
                return new HashSet<>((Collection<String>) rolesObj);
            }
        }
        return new HashSet<>();
    }
}
