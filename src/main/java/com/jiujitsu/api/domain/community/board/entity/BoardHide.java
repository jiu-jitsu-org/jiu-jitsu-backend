package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "board_hide",
        uniqueConstraints = @UniqueConstraint(columnNames = {"created_by", "content_id"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardHide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;
}
