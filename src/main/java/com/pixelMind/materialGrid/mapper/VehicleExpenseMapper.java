package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.VehicleExpenseResponse;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleExpenseMapper {

    private final FileHistoryMapper fileHistoryMapper;

    public VehicleExpenseResponse toResponse(VehicleExpense expense) {
        if (expense == null) {
            return null;
        }
        return VehicleExpenseResponse.builder()
                .id(expense.getId())
                .date(expense.getDate())
                .expenses(expense.getExpenses())
                .amount(expense.getExpenses())
                .vehicleId(expense.getVehicle().getId())
                .vehicleNumber(expense.getVehicle().getVehicleNumber())
                .fileHistoryId(expense.getFileHistory() != null ? expense.getFileHistory().getId() : null)
                .fileHistory(fileHistoryMapper.toResponse(expense.getFileHistory()))
                .createdBy(expense.getCreatedBy())
                .createdDate(expense.getCreatedDate())
                .modifiedBy(expense.getModifiedBy())
                .modifiedDate(expense.getModifiedDate())
                .build();
    }
}
