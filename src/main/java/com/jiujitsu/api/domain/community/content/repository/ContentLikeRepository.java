package com.jiujitsu.api.domain.community.content.repository;

import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {
    Optional<ContentLike> findByContentIdAndCreatedBy(Long contentId, User user);

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
