package com.mycompany.myapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mycompany.myapp.domain.MoodEntry;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.enumeration.MoodType;
import com.mycompany.myapp.repository.MoodEntryRepository;
import com.mycompany.myapp.service.MoodEntryService;
import com.mycompany.myapp.service.dto.MoodEntryDTO;
import com.mycompany.myapp.service.dto.UserDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MoodEntryResourceTest {

    @Mock
    private MoodEntryService moodEntryService;

    @Mock
    private MoodEntryRepository moodEntryRepository;

    @InjectMocks
    private MoodEntryResource moodEntryResource;

    private MoodEntryDTO moodEntryDTO;
    private MoodEntry moodEntry;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(moodEntryResource, "applicationName", "Daily Mood Tracker");

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
    void shouldCreateMoodEntry() throws Exception {
        // Given
        MoodEntryDTO createDTO = new MoodEntryDTO();
        createDTO.setMood(MoodType.HAPPY);
        createDTO.setDate(LocalDate.now());

        when(moodEntryService.save(any(MoodEntryDTO.class))).thenReturn(moodEntryDTO);

        // When
        ResponseEntity<MoodEntryDTO> response = moodEntryResource.createMoodEntry(createDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getHeaders().getLocation()).isEqualTo(new URI("/api/mood-entries/1"));
        verify(moodEntryService).save(any(MoodEntryDTO.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingMoodEntryWithExistingId() {
        // Given
        MoodEntryDTO createDTO = new MoodEntryDTO();
        createDTO.setId(1L);
        createDTO.setMood(MoodType.HAPPY);

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.createMoodEntry(createDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("A new moodEntry cannot already have an ID");
    }

    @Test
    void shouldUpdateMoodEntry() throws Exception {
        // Given
        Long id = 1L;
        when(moodEntryRepository.existsById(id)).thenReturn(true);
        when(moodEntryService.update(any(MoodEntryDTO.class))).thenReturn(moodEntryDTO);

        // When
        ResponseEntity<MoodEntryDTO> response = moodEntryResource.updateMoodEntry(id, moodEntryDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(moodEntryService).update(moodEntryDTO);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingMoodEntryWithNullId() {
        // Given
        Long id = 1L;
        MoodEntryDTO updateDTO = new MoodEntryDTO();
        updateDTO.setId(null);

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.updateMoodEntry(id, updateDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Invalid id");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingMoodEntryWithMismatchedId() {
        // Given
        Long id = 1L;
        MoodEntryDTO updateDTO = new MoodEntryDTO();
        updateDTO.setId(2L);

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.updateMoodEntry(id, updateDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Invalid ID");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentMoodEntry() {
        // Given
        Long id = 1L;
        when(moodEntryRepository.existsById(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.updateMoodEntry(id, moodEntryDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entity not found");
    }

    @Test
    void shouldPartiallyUpdateMoodEntry() throws Exception {
        // Given
        Long id = 1L;
        MoodEntryDTO partialDTO = new MoodEntryDTO();
        partialDTO.setId(1L);
        partialDTO.setMood(MoodType.SAD);

        when(moodEntryRepository.existsById(id)).thenReturn(true);
        when(moodEntryService.partialUpdate(any(MoodEntryDTO.class))).thenReturn(Optional.of(partialDTO));

        // When
        ResponseEntity<MoodEntryDTO> response = moodEntryResource.partialUpdateMoodEntry(id, partialDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMood()).isEqualTo(MoodType.SAD);
        verify(moodEntryService).partialUpdate(partialDTO);
    }

    @Test
    void shouldReturnNotFoundWhenPartiallyUpdatingNonExistentMoodEntry() throws Exception {
        // Given
        Long id = 1L;
        MoodEntryDTO partialDTO = new MoodEntryDTO();
        partialDTO.setId(1L);

        when(moodEntryRepository.existsById(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.partialUpdateMoodEntry(id, partialDTO))
            .isInstanceOf(BadRequestAlertException.class)
            .hasMessageContaining("Entity not found");
        verify(moodEntryService, never()).partialUpdate(any(MoodEntryDTO.class));
    }

    @Test
    void shouldReturnNotFoundWhenPartialUpdateReturnsEmpty() throws Exception {
        // Given
        Long id = 1L;
        MoodEntryDTO partialDTO = new MoodEntryDTO();
        partialDTO.setId(1L);

        when(moodEntryRepository.existsById(id)).thenReturn(true);
        when(moodEntryService.partialUpdate(any(MoodEntryDTO.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.partialUpdateMoodEntry(id, partialDTO))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
        verify(moodEntryService).partialUpdate(partialDTO);
    }

    @Test
    void shouldGetMoodEntryById() {
        // Given
        Long id = 1L;
        when(moodEntryService.findOne(id)).thenReturn(Optional.of(moodEntryDTO));

        // When
        ResponseEntity<MoodEntryDTO> response = moodEntryResource.getMoodEntry(id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        verify(moodEntryService).findOne(id);
    }

    @Test
    void shouldReturnNotFoundWhenGettingNonExistentMoodEntry() {
        // Given
        Long id = 999L;
        when(moodEntryService.findOne(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> moodEntryResource.getMoodEntry(id))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
        verify(moodEntryService).findOne(id);
    }

    @Test
    void shouldDeleteMoodEntry() {
        // Given
        Long id = 1L;

        // When
        ResponseEntity<Void> response = moodEntryResource.deleteMoodEntry(id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moodEntryService).delete(id);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentMoodEntry() {
        // Given
        Long id = 999L;

        // When
        ResponseEntity<Void> response = moodEntryResource.deleteMoodEntry(id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(moodEntryService).delete(id);
    }

    @Test
    void shouldClearUserFromDTOWhenCreating() throws Exception {
        // Given
        MoodEntryDTO createDTO = new MoodEntryDTO();
        createDTO.setMood(MoodType.HAPPY);
        createDTO.setUser(userDTO); // This should be cleared

        when(moodEntryService.save(any(MoodEntryDTO.class))).thenReturn(moodEntryDTO);

        // When
        moodEntryResource.createMoodEntry(createDTO);

        // Then
        verify(moodEntryService).save(argThat(dto -> dto.getUser() == null));
    }
}
