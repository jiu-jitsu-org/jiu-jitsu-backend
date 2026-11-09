package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
// 대회 관심 = (대회, 유저) 한 쌍. 관심 수는 저장하지 않고 이 테이블을 집계한다
// 유니크 (competition_id, user_id) 가 competition_id 선두 인덱스 역할(관심 수 집계)도 하고, 내 관심 조회용으로 user_id 인덱스를 둔다
@Table(name = "competition_interests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_competition_interest_competition_user", columnNames = {"competition_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_competition_interest_user", columnList = "user_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 누가 관심 등록했는지는 도메인 값이라 BaseEntity 의 감사 필드(created_by, 유저 삭제 시 SET_NULL)가 아닌 user 로 둔다
// 대회·유저가 지워지면 관심도 함께 지운다
public class CompetitionInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competition_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Competition competition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public CompetitionInterest(Competition competition, User user) {
        this.competition = competition;
        this.user = user;
    }
}
