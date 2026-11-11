package com.jiujitsu.api.domain.community.comment.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "comment_like",
        indexes = {
                @Index(name = "idx_comment_like", columnList = "comment_id")
        },
        // 한 사람이 한 댓글에 좋아요 하나 (#199). 이름을 고정해 dev 배포 후 생성 여부를 확인할 수 있게 한다
        uniqueConstraints = @UniqueConstraint(name = CommentLike.UNIQUE_CONSTRAINT, columnNames = {"comment_id", "created_by"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommentLike extends BaseEntity {

    public static final String UNIQUE_CONSTRAINT = "uk_comment_like_comment_user";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityComments comment;
}
