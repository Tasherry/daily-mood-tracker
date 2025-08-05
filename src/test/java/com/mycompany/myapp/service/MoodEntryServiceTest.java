package com.mycompany.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.MoodEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.MoodType;
import com.mycompany.myapp.repository.MoodEntryRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.MoodEntryDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.service.mapper.MoodEntryMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MoodEntryServiceTest {

    @Mock
    private MoodEntryRepository moodEntryRepository;

    @Mock
    private MoodEntryMapper moodEntryMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MoodEntryService moodEntryService;

    private MoodEntry moodEntry;
    private MoodEntryDTO moodEntryDTO;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLogin("testuser");
        user.setEmail("test@example.com");

        userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setLogin("testuser");

        moodEntry = new MoodEntry();
        moodEntry.setId(1L);
        moodEntry.setMood(MoodType.HAPPY);
        moodEntry.setDate(LocalDate.now());
        moodEntry.setUser(user);

        moodEntryDTO = new MoodEntryDTO();
        moodEntryDTO.setId(1L);
        moodEntryDTO.setMood(MoodType.HAPPY);
        moodEntryDTO.setDate(LocalDate.now());
        moodEntryDTO.setUser(userDTO);
    }

    @Test
    void shouldSaveMoodEntry() {
        // Given
        when(moodEntryMapper.toEntity(moodEntryDTO)).thenReturn(moodEntry);
        when(moodEntryRepository.save(moodEntry)).thenReturn(moodEntry);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(userRepository.findOneByLogin("testuser")).thenReturn(Optional.of(user));

            // When
            MoodEntryDTO result = moodEntryService.save(moodEntryDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getMood()).isEqualTo(MoodType.HAPPY);
            verify(moodEntryRepository).save(moodEntry);
            verify(moodEntryMapper).toEntity(moodEntryDTO);
            verify(moodEntryMapper).toDto(moodEntry);
        }
    }

    @Test
    void shouldUpdateMoodEntry() {
        // Given
        when(moodEntryMapper.toEntity(moodEntryDTO)).thenReturn(moodEntry);
        when(moodEntryRepository.save(moodEntry)).thenReturn(moodEntry);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        // When
        MoodEntryDTO result = moodEntryService.update(moodEntryDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(moodEntryRepository).save(moodEntry);
        verify(moodEntryMapper).toEntity(moodEntryDTO);
        verify(moodEntryMapper).toDto(moodEntry);
    }

    @Test
    void shouldPartiallyUpdateMoodEntry() {
        // Given
        MoodEntryDTO partialDTO = new MoodEntryDTO();
        partialDTO.setId(1L);
        partialDTO.setMood(MoodType.SAD);

        when(moodEntryRepository.findById(1L)).thenReturn(Optional.of(moodEntry));
        when(moodEntryRepository.save(moodEntry)).thenReturn(moodEntry);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(partialDTO);

        // When
        Optional<MoodEntryDTO> result = moodEntryService.partialUpdate(partialDTO);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getMood()).isEqualTo(MoodType.SAD);
        verify(moodEntryRepository).findById(1L);
        verify(moodEntryMapper).partialUpdate(moodEntry, partialDTO);
        verify(moodEntryRepository).save(moodEntry);
    }

    @Test
    void shouldReturnEmptyWhenPartiallyUpdatingNonExistentMoodEntry() {
        // Given
        MoodEntryDTO partialDTO = new MoodEntryDTO();
        partialDTO.setId(999L);

        when(moodEntryRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<MoodEntryDTO> result = moodEntryService.partialUpdate(partialDTO);

        // Then
        assertThat(result).isEmpty();
        verify(moodEntryRepository).findById(999L);
        verify(moodEntryRepository, never()).save(any());
    }

    @Test
    void shouldFindAllMoodEntries() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<MoodEntry> moodEntries = Arrays.asList(moodEntry);
        Page<MoodEntry> moodEntryPage = new PageImpl<>(moodEntries, pageable, 1);

        when(moodEntryRepository.findAll(pageable)).thenReturn(moodEntryPage);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        // When
        Page<MoodEntryDTO> result = moodEntryService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(moodEntryRepository).findAll(pageable);
    }

    @Test
    void shouldFindAllWithEagerRelationships() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<MoodEntry> moodEntries = Arrays.asList(moodEntry);
        Page<MoodEntry> moodEntryPage = new PageImpl<>(moodEntries, pageable, 1);

        when(moodEntryRepository.findAllWithEagerRelationships(pageable)).thenReturn(moodEntryPage);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        // When
        Page<MoodEntryDTO> result = moodEntryService.findAllWithEagerRelationships(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(moodEntryRepository).findAllWithEagerRelationships(pageable);
    }

    @Test
    void shouldFindOneMoodEntry() {
        // Given
        when(moodEntryRepository.findOneWithEagerRelationships(1L)).thenReturn(Optional.of(moodEntry));
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        // When
        Optional<MoodEntryDTO> result = moodEntryService.findOne(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getId()).isEqualTo(1L);
        verify(moodEntryRepository).findOneWithEagerRelationships(1L);
    }

    @Test
    void shouldReturnEmptyWhenFindingNonExistentMoodEntry() {
        // Given
        when(moodEntryRepository.findOneWithEagerRelationships(999L)).thenReturn(Optional.empty());

        // When
        Optional<MoodEntryDTO> result = moodEntryService.findOne(999L);

        // Then
        assertThat(result).isEmpty();
        verify(moodEntryRepository).findOneWithEagerRelationships(999L);
    }

    @Test
    void shouldDeleteMoodEntry() {
        // When
        moodEntryService.delete(1L);

        // Then
        verify(moodEntryRepository).deleteById(1L);
    }

    @Test
    void shouldFindAllForCurrentUserWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<MoodEntry> moodEntries = Arrays.asList(moodEntry);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(moodEntryRepository.findByUserIsCurrentUser()).thenReturn(moodEntries);
            when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

            // When
            Page<MoodEntryDTO> result = moodEntryService.findAllForCurrentUser(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(moodEntryRepository).findByUserIsCurrentUser();
        }
    }

    @Test
    void shouldFindAllForCurrentUser() {
        // Given
        List<MoodEntry> moodEntries = Arrays.asList(moodEntry);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("testuser"));
            when(moodEntryRepository.findByUserIsCurrentUser()).thenReturn(moodEntries);
            when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

            // When
            List<MoodEntryDTO> result = moodEntryService.findAllForCurrentUser();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(moodEntryRepository).findByUserIsCurrentUser();
        }
    }

    @Test
    void shouldHandleNullUserWhenSaving() {
        // Given
        when(moodEntryMapper.toEntity(moodEntryDTO)).thenReturn(moodEntry);
        when(moodEntryRepository.save(moodEntry)).thenReturn(moodEntry);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.empty());

            // When
            MoodEntryDTO result = moodEntryService.save(moodEntryDTO);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository, never()).findOneByLogin(any());
        }
    }

    @Test
    void shouldHandleUserNotFoundWhenSaving() {
        // Given
        when(moodEntryMapper.toEntity(moodEntryDTO)).thenReturn(moodEntry);
        when(moodEntryRepository.save(moodEntry)).thenReturn(moodEntry);
        when(moodEntryMapper.toDto(moodEntry)).thenReturn(moodEntryDTO);

        try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
            securityUtilsMock.when(SecurityUtils::getCurrentUserLogin).thenReturn(Optional.of("nonexistent"));
            when(userRepository.findOneByLogin("nonexistent")).thenReturn(Optional.empty());

            // When
            MoodEntryDTO result = moodEntryService.save(moodEntryDTO);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findOneByLogin("nonexistent");
        }
    }
}
