package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.AdminUserDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.jhipster.security.RandomUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UserService userService;

    private User user;
    private AdminUserDTO adminUserDTO;
    private Authority authority;

    @BeforeEach
    void setUp() {
        authority = new Authority();
        authority.setName("ROLE_USER");

        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(true);
        user.setAuthorities(Set.of(authority));

        adminUserDTO = new AdminUserDTO();
        adminUserDTO.setId(1L);
        adminUserDTO.setLogin("testuser");
        adminUserDTO.setEmail("test@example.com");
        adminUserDTO.setFirstName("Test");
        adminUserDTO.setLastName("User");
        adminUserDTO.setActivated(true);
        adminUserDTO.setAuthorities(Set.of("ROLE_USER"));
    }

    @Test
    void shouldActivateRegistration() {
        // Given
        String activationKey = "activation-key";
        user.setActivated(false);
        user.setActivationKey(activationKey);

        when(userRepository.findOneByActivationKey(activationKey)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        Optional<User> result = userService.activateRegistration(activationKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().isActivated()).isTrue();
        assertThat(result.orElseThrow().getActivationKey()).isNull();
        verify(userRepository).findOneByActivationKey(activationKey);
        verify(cacheManager).getCache("usersByLogin");
        verify(cacheManager).getCache("usersByEmail");
    }

    @Test
    void shouldReturnEmptyWhenActivationKeyNotFound() {
        // Given
        String activationKey = "invalid-key";
        when(userRepository.findOneByActivationKey(activationKey)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.activateRegistration(activationKey);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findOneByActivationKey(activationKey);
    }

    @Test
    void shouldCompletePasswordReset() {
        // Given
        String resetKey = "reset-key";
        String newPassword = "newPassword";
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now());

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getResetKey()).isNull();
        assertThat(result.orElseThrow().getResetDate()).isNull();
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).findOneByResetKey(resetKey);
    }

    @Test
    void shouldReturnEmptyWhenResetKeyExpired() {
        // Given
        String resetKey = "expired-key";
        String newPassword = "newPassword";
        user.setResetKey(resetKey);
        user.setResetDate(Instant.now().minus(2, ChronoUnit.DAYS));

        when(userRepository.findOneByResetKey(resetKey)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.completePasswordReset(newPassword, resetKey);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findOneByResetKey(resetKey);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldRequestPasswordReset() {
        // Given
        String email = "test@example.com";
        user.setActivated(true);

        try (MockedStatic<RandomUtil> randomUtilMock = mockStatic(RandomUtil.class)) {
            randomUtilMock.when(RandomUtil::generateResetKey).thenReturn("generated-key");

            when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
            when(cacheManager.getCache(anyString())).thenReturn(cache);

            // When
            Optional<User> result = userService.requestPasswordReset(email);

            // Then
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().getResetKey()).isEqualTo("generated-key");
            assertThat(result.orElseThrow().getResetDate()).isNotNull();
            verify(userRepository).findOneByEmailIgnoreCase(email);
        }
    }

    @Test
    void shouldReturnEmptyWhenUserNotActivatedForPasswordReset() {
        // Given
        String email = "test@example.com";
        user.setActivated(false);

        when(userRepository.findOneByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.requestPasswordReset(email);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findOneByEmailIgnoreCase(email);
    }

    @Test
    void shouldRegisterUser() {
        // Given
        String password = "password";
        user.setActivated(false);
        user.setActivationKey("activation-key");

        when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(authorityRepository.findById("ROLE_USER")).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        User result = userService.registerUser(adminUserDTO, password);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo("testuser");
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRemoveNonActivatedUserWhenRegistering() {
        // Given
        String password = "password";
        User existingUser = new User();
        existingUser.setActivated(false);
        existingUser.setLogin("testuser");

        when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.findOneByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(authorityRepository.findById("ROLE_USER")).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        User result = userService.registerUser(adminUserDTO, password);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).delete(existingUser);
    }

    @Test
    void shouldCreateUser() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(authorityRepository.findById("ROLE_USER")).thenReturn(Optional.of(authority));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        User result = userService.createUser(adminUserDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogin()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldUpdateUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        Optional<AdminUserDTO> result = userService.updateUser(adminUserDTO);

        // Then
        assertThat(result).isPresent();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldDeleteUser() {
        // Given
        when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        userService.deleteUser("testuser");

        // Then
        verify(userRepository).findOneByLogin("testuser");
        verify(userRepository).delete(user);
    }

    @Test
    void shouldUpdateUserProfile() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(cacheManager.getCache(anyString())).thenReturn(cache);

            // When
            userService.updateUser("NewFirst", "NewLast", "new@example.com", "en", "new-image.jpg");

            // Then
            verify(userRepository).findOneByLogin("testuser");
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    void shouldChangePassword() {
        // Given
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        user.setPassword("currentEncryptedPassword");

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(currentPassword, "currentEncryptedPassword")).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
            when(cacheManager.getCache(anyString())).thenReturn(cache);

            // When
            userService.changePassword(currentPassword, newPassword);

            // Then
            verify(passwordEncoder).matches(currentPassword, "currentEncryptedPassword");
            verify(passwordEncoder).encode(newPassword);
            // Note: The implementation doesn't call save, it only sets the password and clears cache
        }
    }

    @Test
    void shouldGetAllManagedUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // When
        Page<AdminUserDTO> result = userService.getAllManagedUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findAll(pageable);
    }

    @Test
    void shouldGetAllPublicUsers() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(user);
        Page<User> userPage = new PageImpl<>(users, pageable, 1);

        when(userRepository.findAllByIdNotNullAndActivatedIsTrue(pageable)).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getAllPublicUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findAllByIdNotNullAndActivatedIsTrue(pageable);
    }

    @Test
    void shouldGetUserWithAuthoritiesByLogin() {
        // Given
        when(userRepository.findOneWithAuthoritiesByLogin("testuser")).thenReturn(Optional.of(user));

        // When
        Optional<User> result = userService.getUserWithAuthoritiesByLogin("testuser");

        // Then
        assertThat(result).isPresent();
        verify(userRepository).findOneWithAuthoritiesByLogin("testuser");
    }

    @Test
    void shouldGetUserWithAuthorities() {
        // Given
        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneWithAuthoritiesByLogin("testuser")).thenReturn(Optional.of(user));

            // When
            Optional<User> result = userService.getUserWithAuthorities();

            // Then
            assertThat(result).isPresent();
            verify(userRepository).findOneWithAuthoritiesByLogin("testuser");
        }
    }

    @Test
    void shouldGetAuthorities() {
        // Given
        List<Authority> authorities = Arrays.asList(authority);
        when(authorityRepository.findAll()).thenReturn(authorities);

        // When
        List<String> result = userService.getAuthorities();

        // Then
        assertThat(result).contains("ROLE_USER");
        verify(authorityRepository).findAll();
    }

    @Test
    void shouldRemoveNotActivatedUsers() {
        // Given
        List<User> notActivatedUsers = Arrays.asList(user);
        when(userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(any(Instant.class))).thenReturn(
            notActivatedUsers
        );
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        // When
        userService.removeNotActivatedUsers();

        // Then
        verify(userRepository).findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(any(Instant.class));
        verify(userRepository).delete(user);
    }
}
