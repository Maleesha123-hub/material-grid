package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.Land;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LandRepository extends JpaRepository<Land, Long> {

    Optional<Land> findByLandCode(String landCode);

    List<Land> findByLandCodeIn(Collection<String> landCodes);
}
