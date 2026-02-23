package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private BoardCategory category; // 카테고리

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;    // 컨텐츠 join

    @Column(nullable = false, length = 45)
    private String title;   // 제목

    @Lob
    @Column(nullable = false)
    private String body;    // 내용

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeBody(String body) {
        this.body = body;
    }

    public void changeCategory(BoardCategory category) {
        this.category = category;
    }
}
