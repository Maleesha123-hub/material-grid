package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.FileHistoryResponse;
import com.pixelMind.materialGrid.entity.FileHistory;
import org.springframework.stereotype.Component;

@Component
public class FileHistoryMapper {

    public FileHistoryResponse toResponse(FileHistory fileHistory) {
        if (fileHistory == null) {
            return null;
        }
        return FileHistoryResponse.builder()
                .id(fileHistory.getId())
                .fileName(fileHistory.getFileName())
                .fileType(fileHistory.getFileType().name())
                .uploadedBy(fileHistory.getUploadedBy())
                .uploadedDate(fileHistory.getUploadedDate())
                .build();
    }
}