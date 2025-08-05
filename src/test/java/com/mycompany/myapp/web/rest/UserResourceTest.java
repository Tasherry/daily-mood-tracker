package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the {@link UserResource} REST controller.
 */
@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String DEFAULT_LANGKEY = "en";

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserResource userResource;

    private User user;
    private AdminUserDTO adminUserDTO;

    @BeforeEach
    public void init() {
        user = new User();
        user.setId(1L);
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);

        adminUserDTO = new AdminUserDTO();
        adminUserDTO.setId(1L);
        adminUserDTO.setLogin(DEFAULT_LOGIN);
        adminUserDTO.setFirstName(DEFAULT_FIRSTNAME);
        adminUserDTO.setLastName(DEFAULT_LASTNAME);
        adminUserDTO.setEmail(DEFAULT_EMAIL);
        adminUserDTO.setActivated(true);
        adminUserDTO.setImageUrl(DEFAULT_IMAGEURL);
        adminUserDTO.setLangKey(Constants.DEFAULT_LANGUAGE);
        adminUserDTO.setAuthorities(Collections.singleton(AuthoritiesConstants.USER));
    }

    @Test
    void shouldCreateUser() throws Exception {
        // Given
        when(userService.createUser(any(AdminUserDTO.class))).thenReturn(user);

        // When
        ResponseEntity<User> response = userResource.createUser(adminUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogin()).isEqualTo(DEFAULT_LOGIN);
        verify(userService).createUser(adminUserDTO);
    }

    @Test
    void shouldThrowExceptionWhenCreatingUserWithExistingId() {
        // Given
        adminUserDTO.setId(1L);
        when(userService.createUser(any(AdminUserDTO.class))).thenThrow(
            new BadRequestAlertException("A new user cannot already have an ID", "userManagement", "idexists")
        );

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("A new user cannot already have an ID");
    }

    @Test
    void shouldGetAllUsers() {
        // Given
        List<AdminUserDTO> users = Arrays.asList(adminUserDTO);
        Page<AdminUserDTO> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 20);

        when(userService.getAllManagedUsers(pageable)).thenReturn(userPage);

        // When
        ResponseEntity<List<AdminUserDTO>> response = userResource.getAllUsers(pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        verify(userService).getAllManagedUsers(pageable);
    }

    @Test
    void shouldGetUser() {
        // Given
        when(userService.getUserWithAuthoritiesByLogin(DEFAULT_LOGIN)).thenReturn(Optional.of(user));

        // When
        ResponseEntity<AdminUserDTO> response = userResource.getUser(DEFAULT_LOGIN);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogin()).isEqualTo(DEFAULT_LOGIN);
        verify(userService).getUserWithAuthoritiesByLogin(DEFAULT_LOGIN);
    }

    @Test
    void shouldReturnNotFoundWhenGettingNonExistentUser() {
        // Given
        when(userService.getUserWithAuthoritiesByLogin("unknown")).thenReturn(Optional.empty());

        // When
        ResponseEntity<AdminUserDTO> response = userResource.getUser("unknown");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateUser() {
        // Given
        when(userService.updateUser(any(AdminUserDTO.class))).thenReturn(Optional.of(adminUserDTO));

        // When
        ResponseEntity<AdminUserDTO> response = userResource.updateUser(DEFAULT_LOGIN, adminUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogin()).isEqualTo(DEFAULT_LOGIN);
        verify(userService).updateUser(adminUserDTO);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistentUser() {
        // Given
        when(userService.updateUser(any(AdminUserDTO.class))).thenReturn(Optional.empty());

        // When
        ResponseEntity<AdminUserDTO> response = userResource.updateUser(DEFAULT_LOGIN, adminUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteUser() {
        // Given
        doNothing().when(userService).deleteUser(DEFAULT_LOGIN);

        // When
        ResponseEntity<Void> response = userResource.deleteUser(DEFAULT_LOGIN);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUser(DEFAULT_LOGIN);
    }

    @Test
    void shouldHandleUserCreationWithExistingLogin() {
        // Given
        when(userService.createUser(any(AdminUserDTO.class))).thenThrow(
            new BadRequestAlertException("Login name already used!", "userManagement", "userexists")
        );

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Login name already used!");
    }

    @Test
    void shouldHandleUserCreationWithExistingEmail() {
        // Given
        when(userService.createUser(any(AdminUserDTO.class))).thenThrow(
            new BadRequestAlertException("Email is already in use!", "userManagement", "emailexists")
        );

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Email is already in use!");
    }

    @Test
    void shouldHandleUserUpdateWithExistingLogin() {
        // Given
        when(userService.updateUser(any(AdminUserDTO.class))).thenThrow(
            new BadRequestAlertException("Login name already used!", "userManagement", "userexists")
        );

        // When & Then
        assertThatThrownBy(() -> userResource.updateUser(DEFAULT_LOGIN, adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Login name already used!");
    }

    @Test
    void shouldHandleUserUpdateWithExistingEmail() {
        // Given
        when(userService.updateUser(any(AdminUserDTO.class))).thenThrow(
            new BadRequestAlertException("Email is already in use!", "userManagement", "emailexists")
        );

        // When & Then
        assertThatThrownBy(() -> userResource.updateUser(DEFAULT_LOGIN, adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Email is already in use!");
    }
}
