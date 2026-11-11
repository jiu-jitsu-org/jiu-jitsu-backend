package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.domain.community.Hideable;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "board",
        indexes = {
                @Index(name = "idx_board_competition_id", columnList = "competition_id")   // 대회 이야기 조회
        }
)
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

    // 어떤 대회 이야기인지 (선택, 글 하나에 대회 1개). 카테고리와 따로 — '대회' 카테고리에서만 고르게 하는 건 앱 규칙
    // 대회가 삭제되면 DB 가 연결을 비운다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Competition competition;

    @Column(nullable = false, length = 45)
    private String title;   // 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;    // 내용

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardTag> tags = new ArrayList<>();

    public void addTags(List<BoardTag> newTags) {
        this.tags.addAll(newTags);
    }

    public void replaceTags(List<BoardTag> newTags) {
        this.tags.clear();
        this.tags.addAll(newTags);
    }
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

    // null 이면 연결 해제
    public void changeCompetition(Competition competition) {
        this.competition = competition;
    }

    // 연결된 대회 id (프록시를 초기화하지 않는다)
    public Long getCompetitionId() {
        return competition == null ? null : competition.getId();
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
