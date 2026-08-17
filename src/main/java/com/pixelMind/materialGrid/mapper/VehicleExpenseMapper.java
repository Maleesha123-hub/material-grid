package com.pixelMind.materialGrid.mapper;

import com.pixelMind.materialGrid.dto.response.VehicleExpenseResponse;
import com.pixelMind.materialGrid.entity.VehicleExpense;
import org.springframework.stereotype.Component;

@Component
public class VehicleExpenseMapper {

    public VehicleExpenseResponse toResponse(VehicleExpense expense) {
        if (expense == null) {
            return null;
        }
        return VehicleExpenseResponse.builder()
                .id(expense.getId())
                .date(expense.getDate())
                .expenses(expense.getExpenses())
                .vehicleId(expense.getVehicle().getId())
                .vehicleNumber(expense.getVehicle().getVehicleNumber())
                .createdBy(expense.getCreatedBy())
                .createdDate(expense.getCreatedDate())
                .modifiedBy(expense.getModifiedBy())
                .modifiedDate(expense.getModifiedDate())
                .build();
    }
}
