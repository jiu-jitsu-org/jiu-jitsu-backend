package com.jiujitsu.api.domain.community.comment.entity;

import com.jiujitsu.api.domain.user.entity.User;
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
        name = "community_comment_reaction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_comment_user",
                        columnNames = {"comment_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_reaction_comment", columnList = "comment_id"),
                @Index(name = "idx_reaction_comment_type", columnList = "comment_id, reaction_type"),
//                @Index(name = "idx_reaction_user", columnList = "user_id")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CommunityCommentReaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityComments comment;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "user_id", nullable = false)
//    @OnDelete(action = OnDeleteAction.CASCADE)
//    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 16)
    private CommunityCommentReactionType reactionType;

    public void changeTo(CommunityCommentReactionType newType) {
        this.reactionType = newType;
    }
}
