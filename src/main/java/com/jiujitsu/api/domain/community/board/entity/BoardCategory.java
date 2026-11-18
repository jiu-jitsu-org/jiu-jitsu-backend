package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "board_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_board_category_name", columnNames = "name")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;    // 카테고리명

    @Builder.Default
    @Column(nullable = false, length = 100)
    private String description = "";   // 설명 한 줄 (글쓰기 · 피드 카테고리 탭)

    @Column(nullable = false)
    private int displayOrder;   // 보이는 순서 (작을수록 앞)

    @Column(nullable = false)
    private boolean isActive;   // 사용여부
}

