package com.pixelMind.materialGrid.repository;

import com.pixelMind.materialGrid.entity.FileHistory;
import com.pixelMind.materialGrid.entity.enums.FileType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface FileHistoryRepository extends JpaRepository<FileHistory, Long> {

    boolean existsByFileNameAndFileType(String fileName, FileType fileType);

    /**
     * Same optional-parameter pattern already used by
     * DailyRouteRepository#search - each filter only applies when its
     * argument is non-null, letting the controller pass through whatever
     * subset of query params the caller actually supplied.
     */
    @Query("""
            select f from FileHistory f
            where (:fileName is null or lower(f.fileName) like lower(concat('%', :fileName, '%')))
              and (:fileType is null or f.fileType = :fileType)
              and (:uploadedBy is null or lower(f.uploadedBy) like lower(concat('%', :uploadedBy, '%')))
              and (:fromDate is null or f.uploadedDate >= :fromDate)
              and (:toDate is null or f.uploadedDate < :toDate)
            """)
    Page<FileHistory> search(
            @Param("fileName") String fileName,
            @Param("fileType") FileType fileType,
            @Param("uploadedBy") String uploadedBy,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}