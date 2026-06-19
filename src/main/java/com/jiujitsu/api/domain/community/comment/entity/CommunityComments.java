package com.jiujitsu.api.domain.community.comment.entity;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "community_comments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommunityComments extends BaseEntity implements Hideable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Content content;

    @Column(nullable = false)
    @Builder.Default
    private Long parentId = 0L;  // 대댓글 - comment 연동 (최상위 댓글: 0)

    @Column(nullable = false)
    private String body;        // 댓글 내용

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentLike> likes = new ArrayList<>();   // 좋아요

    @Column
    private LocalDateTime hiddenAt;

    @Column
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isReported = false;

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
        this.isReported = false;
    }

    // 대댓글 있는 댓글을 신고로 숨길 때: hiddenAt 대신 isReported 플래그 사용
    public void markAsReported() {
        this.isReported = true;
    }

    // 대댓글 있는 댓글을 유저가 삭제할 때: hard-delete 대신 soft-delete
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    @Override
    public boolean isHidden() {
        return this.hiddenAt != null || this.isReported;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
