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

    boolean existsByFileNameAndFileTypeAndDeletedFalse(
            String fileName,
            FileType fileType
    );

    @Query("""
            select f from FileHistory f
            where f.deleted = false
              and (:fileName is null or lower(f.fileName) like lower(concat('%', :fileName, '%')))
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