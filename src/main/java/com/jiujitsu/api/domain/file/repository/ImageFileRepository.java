package com.jiujitsu.api.domain.file.repository;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.ImageFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    List<ImageFile> findByStatusAndUpdatedAtBefore(ImageFileStatus status, LocalDateTime threshold);
}
