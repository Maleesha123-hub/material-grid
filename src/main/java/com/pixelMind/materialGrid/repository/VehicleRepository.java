package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumberAndDeletedFalse(String vehicleNumber);

    Optional<Vehicle> findByVehicleNumberAndDeletedFalse(String vehicleNumber);

    Page<Vehicle> findByVehicleNumberContainingIgnoreCaseAndDeletedFalse(
            String vehicleNumber,
            Pageable pageable);

    List<Vehicle> findByVehicleNumberInAndDeletedFalse(
            Collection<String> vehicleNumbers);
}