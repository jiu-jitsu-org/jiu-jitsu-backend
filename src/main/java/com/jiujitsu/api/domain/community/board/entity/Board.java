package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.domain.community.Hideable;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "board")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board extends BaseEntity implements Hideable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private BoardCategory category; // 카테고리

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;    // 컨텐츠 join

    @Column(nullable = false, length = 45)
    private String title;   // 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;    // 내용

    @Column
    private LocalDateTime hiddenAt; // 숨김일시

    // todo : 투표, 태그

    public void changeTitle(String title) {
        this.title = title;
    }

    public void changeBody(String body) {
        this.body = body;
    }

    public void changeCategory(BoardCategory category) {
        this.category = category;
    }

    // 수정권한 체크
    public void validateOwner(User user) {
        if (!Objects.equals(this.getCreatedBy(), user)) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }
    }

    public void hide() {
        this.hiddenAt = LocalDateTime.now();
    }

    public void unhide() {
        this.hiddenAt = null;
    }

    public boolean isHidden() {
        return this.hiddenAt != null;
    }

    // 수정 여부 체크
    public boolean isUpdated() {
        return !Objects.equals(getCreatedAt(), getUpdatedAt());
    }
}
