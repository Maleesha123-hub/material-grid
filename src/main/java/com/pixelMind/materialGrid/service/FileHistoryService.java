package com.pixelMind.materialGrid.service;

import com.pixelMind.materialGrid.dto.request.FileHistoryFilterRequest;
import com.pixelMind.materialGrid.dto.response.FileHistoryResponse;
import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FileHistoryService {

    /**
     * Throws DuplicateFileUploadException if this exact (fileName,
     * fileType) combination has already been uploaded. Call this BEFORE
     * parsing the Excel workbook - see class Javadoc on the impl.
     */
    void validateNotAlreadyUploaded(String fileName, FileType fileType);

    /**
     * Creates and persists the FileHistory row. MUST be called from within
     * the same @Transactional method that will go on to save the imported
     * Vehicle/VehicleExpense/DailyRoute rows - see class Javadoc on the impl
     * for why this deliberately does NOT use REQUIRES_NEW.
     */
    FileHistory createFileHistory(String fileName, FileType fileType);

    Page<FileHistoryResponse> search(FileHistoryFilterRequest filter, Pageable pageable);

    List<FileHistoryResponse> getFilesByFileType(FileType fileType, String fileName);

    FileHistoryResponse getById(Long id);
}