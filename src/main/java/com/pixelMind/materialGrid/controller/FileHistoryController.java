package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.request.FileHistoryFilterRequest;
import com.pixelMind.materialGrid.dto.response.ApiResponse;
import com.pixelMind.materialGrid.dto.response.FileHistoryResponse;
import com.pixelMind.materialGrid.dto.response.PageResponse;
import com.pixelMind.materialGrid.entity.enums.FileType;
import com.pixelMind.materialGrid.service.FileHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only by design - File History records are created exclusively as a
 * side effect of a successful Excel upload (see the three import services).
 * No create/update/delete endpoints exist here, matching the spec's
 * explicit "File History does NOT need normal create/update/delete CRUD
 * from the frontend."
 */
@Tag(name = "File History", description = "Read-only audit log of completed Excel uploads (Vehicle, Vehicle Expense, Daily Route)")
@RestController
@RequestMapping("/api/v1/file-history")
@RequiredArgsConstructor
public class FileHistoryController {

    private final FileHistoryService fileHistoryService;

    @Operation(summary = "Search file upload history (paginated; optional filters: fileName, fileType, uploadedBy, fromDate, toDate)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FileHistoryResponse>>> search(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) FileType fileType,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        FileHistoryFilterRequest filter = new FileHistoryFilterRequest(fileName, fileType, uploadedBy, fromDate,
                toDate);
        PageResponse<FileHistoryResponse> page = new PageResponse<>(fileHistoryService.search(filter, pageable));
        return ResponseEntity.ok(ApiResponse.success("File history retrieved successfully", page));
    }

    @Operation(summary = "Get active file history records (filter by fileType, searchable with LIKE by fileName)")
    @GetMapping("/by-file-type")
    public ResponseEntity<ApiResponse<List<FileHistoryResponse>>> getFilesByFileType(
            @RequestParam(required = false) FileType fileType,
            @RequestParam(required = false) String fileName) {
        return ResponseEntity.ok(ApiResponse.success(
                "File history retrieved successfully",
                fileHistoryService.getFilesByFileType(fileType, fileName)));
    }

    @Operation(summary = "Get a file history record by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileHistoryResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("File history retrieved successfully", fileHistoryService.getById(id)));
    }
}