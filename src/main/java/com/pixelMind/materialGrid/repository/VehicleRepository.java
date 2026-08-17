package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumber(String vehicleNumber);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    Page<Vehicle> findByVehicleNumberContainingIgnoreCase(String vehicleNumber, Pageable pageable);

    // Bulk lookup used by Excel import services - one query for every
    // distinct vehicle number in an uploaded file, instead of one per row.
    List<Vehicle> findByVehicleNumberIn(Collection<String> vehicleNumbers);
}