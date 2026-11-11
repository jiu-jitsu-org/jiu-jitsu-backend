package com.jiujitsu.api.domain.community.content.entity;

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
        name = "content_like",
        indexes = {
                @Index(name = "idx_content_like", columnList = "content_id")
        },
        // 한 사람이 한 게시글에 좋아요 하나. 동시 요청 충돌을 이 이름으로 가린다 (ContentService#like, #199)
        uniqueConstraints = @UniqueConstraint(name = ContentLike.UNIQUE_CONSTRAINT, columnNames = {"content_id", "created_by"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContentLike extends BaseEntity {

    public static final String UNIQUE_CONSTRAINT = "uk_content_like_content_user";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Content content;
}
