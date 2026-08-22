/*
package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.DuplicateFileUploadException;
import com.pixelMind.materialGrid.mapper.FileHistoryMapper;
import com.pixelMind.materialGrid.repository.FileHistoryRepository;
import com.pixelMind.materialGrid.service.impl.FileHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileHistoryServiceImplTest {

    @Mock
    private FileHistoryRepository fileHistoryRepository;
    @Mock
    private FileHistoryMapper fileHistoryMapper;

    @InjectMocks
    private FileHistoryServiceImpl fileHistoryService;

    @Test
    void validateNotAlreadyUploaded_whenNotExists_doesNotThrow() {
        when(fileHistoryRepository.existsByFileNameAndFileType("vehicles.xlsx", FileType.VEHICLE)).thenReturn(false);
        fileHistoryService.validateNotAlreadyUploaded("vehicles.xlsx", FileType.VEHICLE); // no throw
        verify(fileHistoryRepository).existsByFileNameAndFileType("vehicles.xlsx", FileType.VEHICLE);
    }

    @Test
    void validateNotAlreadyUploaded_whenExists_throwsDuplicateFileUploadException() {
        when(fileHistoryRepository.existsByFileNameAndFileType("vehicles.xlsx", FileType.VEHICLE)).thenReturn(true);

        assertThatThrownBy(() -> fileHistoryService.validateNotAlreadyUploaded("vehicles.xlsx", FileType.VEHICLE))
                .isInstanceOf(DuplicateFileUploadException.class)
                .hasMessageContaining("vehicles.xlsx")
                .hasMessageContaining("VEHICLE");
    }

    @Test
    void sameFileName_differentFileType_isTreatedAsDistinct() {
        when(fileHistoryRepository.existsByFileNameAndFileType("vehicles.xlsx", FileType.DAILY_ROUTE)).thenReturn(false);
        fileHistoryService.validateNotAlreadyUploaded("vehicles.xlsx", FileType.DAILY_ROUTE); // no throw despite
        // "vehicles.xlsx" existing for FileType.VEHICLE - the mock above never stubs that combination as true.
        verify(fileHistoryRepository).existsByFileNameAndFileType("vehicles.xlsx", FileType.DAILY_ROUTE);
    }

    @Test
    void createFileHistory_success_returnsSavedEntity() {
        when(fileHistoryRepository.save(any(FileHistory.class))).thenAnswer(inv -> {
            FileHistory fh = inv.getArgument(0);
            fh.setId(1L);
            return fh;
        });

        FileHistory result = fileHistoryService.createFileHistory("vehicles.xlsx", FileType.VEHICLE);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFileName()).isEqualTo("vehicles.xlsx");
        assertThat(result.getFileType()).isEqualTo(FileType.VEHICLE);
        assertThat(result.getUploadedDate()).isNotNull();
    }

    @Test
    void createFileHistory_raceConditionOnUniqueConstraint_convertsToDuplicateFileUploadException() {
        when(fileHistoryRepository.save(any(FileHistory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> fileHistoryService.createFileHistory("vehicles.xlsx", FileType.VEHICLE))
                .isInstanceOf(DuplicateFileUploadException.class);
    }
}*/
