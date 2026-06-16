package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.BoardTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardTagRepository extends JpaRepository<BoardTag, Long> {
}
