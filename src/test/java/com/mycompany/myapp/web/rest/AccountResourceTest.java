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
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.UserService;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.PasswordChangeDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.web.rest.errors.EmailAlreadyUsedException;
import com.mycompany.myapp.web.rest.errors.InvalidPasswordException;
import com.mycompany.myapp.web.rest.errors.LoginAlreadyUsedException;
import com.mycompany.myapp.web.rest.vm.KeyAndPasswordVM;
import com.mycompany.myapp.web.rest.vm.ManagedUserVM;
import java.time.Instant;
import java.util.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the {@link AccountResource} REST controller.
 */
@ExtendWith(MockitoExtension.class)
class AccountResourceTest {

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
    private AccountResource accountResource;

    private User user;
    private AdminUserDTO adminUserDTO;
    private ManagedUserVM managedUserVM;

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

        managedUserVM = new ManagedUserVM();
        managedUserVM.setLogin(DEFAULT_LOGIN);
        managedUserVM.setPassword("password");
        managedUserVM.setFirstName(DEFAULT_FIRSTNAME);
        managedUserVM.setLastName(DEFAULT_LASTNAME);
        managedUserVM.setEmail(DEFAULT_EMAIL);
        managedUserVM.setActivated(true);
        managedUserVM.setImageUrl(DEFAULT_IMAGEURL);
        managedUserVM.setLangKey(Constants.DEFAULT_LANGUAGE);
    }

    @Test
    void shouldGetAccount() {
        // Given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            when(userService.getUserWithAuthorities()).thenReturn(Optional.of(user));

            // When
            AdminUserDTO response = accountResource.getAccount();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getLogin()).isEqualTo(DEFAULT_LOGIN);
            verify(userService).getUserWithAuthorities();
        }
    }

    @Test
    void shouldThrowExceptionWhenGettingAccountWithoutAuthentication() {
        // Given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> accountResource.getAccount())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User could not be found");
        }
    }

    @Test
    void shouldRegisterAccount() {
        // Given
        when(userService.registerUser(any(ManagedUserVM.class), anyString())).thenReturn(user);

        // When
        accountResource.registerAccount(managedUserVM);

        // Then
        verify(userService).registerUser(managedUserVM, managedUserVM.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenRegisteringAccountWithExistingLogin() {
        // Given
        when(userService.registerUser(any(ManagedUserVM.class), anyString())).thenThrow(new LoginAlreadyUsedException());

        // When & Then
        assertThatThrownBy(() -> accountResource.registerAccount(managedUserVM)).isInstanceOf(LoginAlreadyUsedException.class);
    }

    @Test
    void shouldThrowExceptionWhenRegisteringAccountWithExistingEmail() {
        // Given
        when(userService.registerUser(any(ManagedUserVM.class), anyString())).thenThrow(new EmailAlreadyUsedException());

        // When & Then
        assertThatThrownBy(() -> accountResource.registerAccount(managedUserVM)).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void shouldThrowExceptionWhenRegisteringAccountWithInvalidPassword() {
        // Given
        when(userService.registerUser(any(ManagedUserVM.class), anyString())).thenThrow(new InvalidPasswordException());

        // When & Then
        assertThatThrownBy(() -> accountResource.registerAccount(managedUserVM)).isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void shouldActivateAccount() {
        // Given
        String key = "activation-key";
        when(userService.activateRegistration(key)).thenReturn(Optional.of(user));

        // When
        accountResource.activateAccount(key);

        // Then
        verify(userService).activateRegistration(key);
    }

    @Test
    void shouldThrowExceptionWhenActivatingAccountWithInvalidKey() {
        // Given
        String key = "invalid-key";
        when(userService.activateRegistration(key)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountResource.activateAccount(key))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No user was found for this activation key");
    }

    @Test
    void shouldRequestPasswordReset() {
        // Given
        String email = DEFAULT_EMAIL;
        doNothing().when(userService).requestPasswordReset(email);

        // When
        accountResource.requestPasswordReset(email);

        // Then
        verify(userService).requestPasswordReset(email);
    }

    @Test
    void shouldCompletePasswordReset() {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("reset-key");
        keyAndPassword.setNewPassword("new-password");
        when(userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())).thenReturn(Optional.of(user));

        // When
        accountResource.finishPasswordReset(keyAndPassword);

        // Then
        verify(userService).completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
    }

    @Test
    void shouldThrowExceptionWhenCompletingPasswordResetWithInvalidKey() {
        // Given
        KeyAndPasswordVM keyAndPassword = new KeyAndPasswordVM();
        keyAndPassword.setKey("invalid-key");
        keyAndPassword.setNewPassword("new-password");
        when(userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountResource.finishPasswordReset(keyAndPassword))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No user was found for this reset key");
    }

    @Test
    void shouldChangePassword() {
        // Given
        PasswordChangeDTO passwordChange = new PasswordChangeDTO();
        passwordChange.setCurrentPassword("current-password");
        passwordChange.setNewPassword("new-password");
        doNothing().when(userService).changePassword(passwordChange.getCurrentPassword(), passwordChange.getNewPassword());

        // When
        accountResource.changePassword(passwordChange);

        // Then
        verify(userService).changePassword(passwordChange.getCurrentPassword(), passwordChange.getNewPassword());
    }

    @Test
    void shouldSaveAccount() {
        // Given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            doNothing().when(userService).updateUser(any(AdminUserDTO.class));

            // When
            accountResource.saveAccount(adminUserDTO);

            // Then
            verify(userService).updateUser(adminUserDTO);
        }
    }

    @Test
    void shouldThrowExceptionWhenSavingAccountWithExistingEmail() {
        // Given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            doThrow(new EmailAlreadyUsedException()).when(userService).updateUser(any(AdminUserDTO.class));

            // When & Then
            assertThatThrownBy(() -> accountResource.saveAccount(adminUserDTO)).isInstanceOf(EmailAlreadyUsedException.class);
        }
    }

    @Test
    void shouldThrowExceptionWhenSavingAccountWithExistingLogin() {
        // Given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of(DEFAULT_LOGIN));
            doThrow(new LoginAlreadyUsedException()).when(userService).updateUser(any(AdminUserDTO.class));

            // When & Then
            assertThatThrownBy(() -> accountResource.saveAccount(adminUserDTO)).isInstanceOf(LoginAlreadyUsedException.class);
        }
    }
}
