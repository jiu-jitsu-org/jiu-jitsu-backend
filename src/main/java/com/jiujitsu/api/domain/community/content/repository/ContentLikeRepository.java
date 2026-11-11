package com.jiujitsu.api.domain.community.content.repository;

import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {
    // 단건 조회 대신 존재 여부·벌크 삭제를 쓴다: 동시에 취소해 지울 행이 없어도 예외가 나지 않는다 (#199)
    boolean existsByContentIdAndCreatedById(Long contentId, Long userId);

    // 벌크 삭제로 둔다: 파생 deleteBy 는 엔티티를 읽어 한 건씩 지워, 동시 취소로 먼저 지워진 행이면 예외가 난다
    @Modifying
    @Query("delete from ContentLike cl where cl.content.id = :contentId and cl.createdBy.id = :userId")
    void deleteAllByContentIdAndCreatedById(@Param("contentId") Long contentId, @Param("userId") Long userId);

    @Query("""
        select c.content.id, count(c)
        from ContentLike c
        where c.content.id in :contentIds
        group by c.content.id
   \s""")
    List<Object[]> countContentLikeByContentIds(@Param("contentIds") List<Long> contentIds);

    @Query("""
    select distinct c.content.id
    from ContentLike c
    where c.createdBy.id = :userId
      and c.content.id in :contentIds
    """)
    Set<Long> findUserLikedContentIds(@Param("userId") Long userId, @Param("contentIds") List<Long> contentIds);
}
