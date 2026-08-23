package com.pixelMind.materialGrid.dto.request;

import com.pixelMind.materialGrid.entity.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Plain optional-filter holder, populated by the controller from individual
 * @RequestParam values (not @RequestBody) - matching the existing
 * project's convention for list-filtering (see DailyRouteController's
 * query-parameter search), not a JSON request body.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FileHistoryFilterRequest {
    private String fileName;
    private FileType fileType;
    private String uploadedBy;
    private LocalDate fromDate;
    private LocalDate toDate;
}