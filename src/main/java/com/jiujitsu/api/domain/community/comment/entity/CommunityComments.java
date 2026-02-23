package com.jiujitsu.api.domain.community.comment.entity;

import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsUpdateRequest;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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
    private Long id;    // id

    // TODO 25.10.24 게시글 데이블 만들어지면 연결
    @Column(nullable = false)
    private Long postId;

    @Column(nullable = true)
    private Long parentId;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "author_id", nullable = false)
//    @OnDelete(action = OnDeleteAction.CASCADE)
//    private User author; // 유저정보

    @Column
    private String body;        // 댓글 내용

    public void updateFrom(CommunityCommentsUpdateRequest request, User author) {
//        this.author = author;
        this.body = request.body();
    }
}
