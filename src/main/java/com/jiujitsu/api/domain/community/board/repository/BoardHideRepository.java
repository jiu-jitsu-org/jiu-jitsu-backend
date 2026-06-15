package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.BoardHide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardHideRepository extends JpaRepository<BoardHide, Long> {

    Optional<BoardHide> findByCreatedByIdAndContentId(Long userId, Long contentId);

    @Query("SELECT bh.contentId FROM BoardHide bh WHERE bh.createdBy.id = :userId")
    List<Long> findContentIdsByUserId(@Param("userId") Long userId);
}
