package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContentNoticeSettingRepository extends JpaRepository<ContentNoticeSetting, Long> {
    Optional<ContentNoticeSetting> findByUserIdAndContentId(Long userId, Long contentId);

    List<ContentNoticeSetting> findByUserIdAndContentIdIn(Long userId, Collection<Long> contentIds);
}
