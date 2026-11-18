package com.jiujitsu.api.domain.community.content.repository;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {

    @Query("SELECT c FROM Content c JOIN c.imageFiles i WHERE i = :imageFile")
    List<Content> findByImageFile(@Param("imageFile") ImageFile imageFile);

    // 조회수 (#249): '봤음' 요청에서만 DB 에서 +1 — 동시에 열어도 덜 세지 않고, 수정자 · 수정 시각(감사 필드)도 바뀌지 않는다
    @Modifying
    @Query("UPDATE Content c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    int incrementViewCount(@Param("id") Long id);

    // 작성자는 첫 1회만 — 이미 셌으면 0행
    @Modifying
    @Query("UPDATE Content c SET c.viewCount = c.viewCount + 1, c.authorViewCounted = true"
            + " WHERE c.id = :id AND c.authorViewCounted = false")
    int incrementViewCountForAuthor(@Param("id") Long id);

    @Query("SELECT c.viewCount FROM Content c WHERE c.id = :id")
    long findViewCountById(@Param("id") Long id);
}
