package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import com.mycompany.myapp.web.rest.errors.EmailAlreadyUsedException;
import com.mycompany.myapp.web.rest.errors.LoginAlreadyUsedException;
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

    @Mock
    private com.mycompany.myapp.service.MailService mailService;

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
        adminUserDTO.setId(null); // Important: set to null for new user creation
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
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(userService.createUser(any(AdminUserDTO.class))).thenReturn(user);
        doNothing().when(mailService).sendCreationEmail(any(User.class));

        // When
        ResponseEntity<User> response = userResource.createUser(adminUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLogin()).isEqualTo(DEFAULT_LOGIN);
        verify(userService).createUser(adminUserDTO);
        verify(mailService).sendCreationEmail(user);
    }

    @Test
    void shouldThrowExceptionWhenCreatingUserWithExistingId() {
        // Given
        adminUserDTO.setId(1L);

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("A new user cannot already have an ID");
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

        // When & Then
        assertThatThrownBy(() -> userResource.getUser("unknown"))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateUser() {
        // Given
        adminUserDTO.setId(1L);
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
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
        adminUserDTO.setId(1L);
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
        when(userService.updateUser(any(AdminUserDTO.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userResource.updateUser(DEFAULT_LOGIN, adminUserDTO))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
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
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO)).isInstanceOf(LoginAlreadyUsedException.class);
    }

    @Test
    void shouldHandleUserCreationWithExistingEmail() {
        // Given
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> userResource.createUser(adminUserDTO)).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void shouldHandleUserUpdateWithExistingLogin() {
        // Given
        adminUserDTO.setId(1L);
        User existingUser = new User();
        existingUser.setId(2L); // Different ID
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin(DEFAULT_LOGIN.toLowerCase())).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userResource.updateUser(DEFAULT_LOGIN, adminUserDTO)).isInstanceOf(LoginAlreadyUsedException.class);
    }

    @Test
    void shouldHandleUserUpdateWithExistingEmail() {
        // Given
        adminUserDTO.setId(1L);
        User existingUser = new User();
        existingUser.setId(2L); // Different ID
        when(userRepository.findOneByEmailIgnoreCase(DEFAULT_EMAIL)).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userResource.updateUser(DEFAULT_LOGIN, adminUserDTO)).isInstanceOf(EmailAlreadyUsedException.class);
    }
}
