package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByVehicleNumber(String vehicleNumber);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    Page<Vehicle> findByVehicleNumberContainingIgnoreCase(String vehicleNumber, Pageable pageable);

    List<Vehicle> findTop20ByVehicleNumberContainingIgnoreCaseOrderByVehicleNumberAsc(String vehicleNumber);

    List<Vehicle> findByVehicleNumberIn(Collection<String> vehicleNumbers);
}