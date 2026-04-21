package com.jiujitsu.api.domain.community.comment.entity;

import com.jiujitsu.api.domain.community.content.entity.Content;
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
@Table(name = "community_comments")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommunityComments extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Content content;

    @Column
    private Long parentId;  // 대댓글 - comment 연동

    @Column(nullable = false)
    private String body;        // 댓글 내용
}
