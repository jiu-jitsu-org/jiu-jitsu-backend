package com.jiujitsu.api.domain.file.repository;

import com.jiujitsu.api.domain.file.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {
}
