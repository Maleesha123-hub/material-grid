package com.pixelMind.materialGrid.repository.dailyRoutes;

import com.pixelMind.materialGrid.entity.dailyRoutes.DailyRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyRouteRepository extends JpaRepository<DailyRoute, Long> {

    @Query("SELECT d FROM DailyRoute d " +
           "LEFT JOIN FETCH d.vehicle v " +
           "LEFT JOIN FETCH d.route r " +
           "LEFT JOIN FETCH d.land l " +
           "WHERE d.date = :date " +
           "AND UPPER(TRIM(v.vehicleNumber)) = UPPER(TRIM(:vehicleNumber)) " +
           "AND d.active = true " +
           "ORDER BY d.id ASC")
    List<DailyRoute> findByDateAndVehicleNumber(@Param("date") LocalDate date, @Param("vehicleNumber") String vehicleNumber);

    List<DailyRoute> findByDateAndActiveTrue(LocalDate date);
}
