package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.dto.request.FileHistoryFilterRequest;
import com.pixelMind.materialGrid.dto.response.FileHistoryResponse;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.exception.DuplicateFileUploadException;
import com.pixelMind.materialGrid.exception.ResourceNotFoundException;
import com.pixelMind.materialGrid.mapper.FileHistoryMapper;
import com.pixelMind.materialGrid.repository.FileHistoryRepository;
import com.pixelMind.materialGrid.service.FileHistoryService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import com.pixelMind.materialGrid.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralized owner of all File History logic - the three Excel import
 * services (Vehicle, VehicleExpense, DailyRoute) call into this rather than
 * each re-implementing existsByFileNameAndFileType() themselves (see the
 * "IMPORTANT ARCHITECTURAL REQUIREMENT" in the feature spec).
 *
 * TRANSACTION NOTE - the most important detail in this class:
 * createFileHistory() uses the DEFAULT (REQUIRED) propagation, NOT
 * REQUIRES_NEW. This is the opposite of CodeGeneratorService (used for
 * Route/License codes), which intentionally isolates its counter increment
 * in its own transaction so a rolled-back caller doesn't undo the
 * reservation. Here, the requirement is the exact opposite: a FileHistory
 * row must NEVER survive if the Vehicle/VehicleExpense/DailyRoute rows that
 * were supposed to reference it fail to save. Using REQUIRES_NEW here would
 * be a bug - it would let a FileHistory row exist with zero corresponding
 * uploaded records after a failure. REQUIRED (participating in the caller's
 * existing @Transactional method) is what makes them commit or roll back
 * together.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileHistoryServiceImpl implements FileHistoryService {

    private final FileHistoryRepository fileHistoryRepository;
    private final FileHistoryMapper fileHistoryMapper;

    @Override
    @Transactional(readOnly = true)
    public void validateNotAlreadyUploaded(String fileName, FileType fileType) {
        if (fileHistoryRepository.existsByFileNameAndFileTypeAndDeletedFalse(fileName, fileType)) {
            throw duplicateException(fileName, fileType);
        }
    }

    @Override
    @Transactional
    public FileHistory createFileHistory(String fileName, FileType fileType) {
        FileHistory fileHistory = FileHistory.builder()
                .fileName(fileName)
                .fileType(fileType)
                .uploadedBy(SecurityUtil.getCurrentUsername())
                .uploadedDate(DateTimeUtil.nowUtc())
                .build();
        try {
            FileHistory saved = fileHistoryRepository.save(fileHistory);
            log.info("File history created: id={}, fileName={}, fileType={}, uploadedBy={}",
                    saved.getId(), fileName, fileType, saved.getUploadedBy());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Race-condition backstop: two concurrent uploads of the same
            // (fileName, fileType) both passed validateNotAlreadyUploaded's
            // read check before either committed. The DB unique constraint
            // (see uk_file_history_name_type) catches what the read check
            // couldn't, and we convert the raw constraint violation into
            // the same clean business error rather than leaking it - see
            // spec section 31.
            log.warn("Duplicate file upload race detected for fileName={}, fileType={}", fileName, fileType);
            throw duplicateException(fileName, fileType);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileHistoryResponse> search(FileHistoryFilterRequest filter, Pageable pageable) {
        LocalDateTime from = filter.getFromDate() != null ? filter.getFromDate().atStartOfDay() : null;
        LocalDateTime to = filter.getToDate() != null ? filter.getToDate().plusDays(1).atStartOfDay() : null;

        return fileHistoryRepository
                .search(filter.getFileName(), filter.getFileType(), filter.getUploadedBy(), from, to, pageable)
                .map(fileHistoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileHistoryResponse> getFilesByFileType(FileType fileType, String fileName) {
        String cleanFileName = (fileName != null && !fileName.isBlank()) ? fileName.trim() : null;
        return fileHistoryRepository.findByFileTypeAndFileName(fileType, cleanFileName).stream()
                .map(fileHistoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileHistoryResponse getById(Long id) {
        FileHistory fileHistory = fileHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File history not found with id: " + id, ErrorCodeConstants.FILE_HISTORY_NOT_FOUND));
        return fileHistoryMapper.toResponse(fileHistory);
    }

    private DuplicateFileUploadException duplicateException(String fileName, FileType fileType) {
        return new DuplicateFileUploadException(
                "File '" + fileName + "' has already been uploaded as " + fileType + ".",
                ErrorCodeConstants.DUPLICATE_FILE_UPLOAD);
    }
}