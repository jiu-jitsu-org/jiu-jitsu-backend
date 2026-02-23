package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Page<Board> findAllByCategory_Id(Long categoryId, Pageable pageable);
}
