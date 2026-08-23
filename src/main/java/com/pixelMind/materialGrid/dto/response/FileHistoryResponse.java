package com.pixelMind.materialGrid.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class FileHistoryResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String uploadedBy;
    private LocalDateTime uploadedDate;
}