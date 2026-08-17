package com.pixelMind.materialGrid.service.dailyRoutes;

import com.pixelMind.materialGrid.dto.response.CommonResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DailyRouteImportService {

    /**
     * This method is allowed to bulk upload of daily routes
     *
     * @param file
     * @return
     */
    CommonResponseDTO bulkUpload(MultipartFile file);
}