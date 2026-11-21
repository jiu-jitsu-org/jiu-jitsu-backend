package com.jiujitsu.api.domain.community.board.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "board_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_board_category_name", columnNames = "name")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;    // 카테고리명

    @Builder.Default
    @Column(nullable = false, length = 100)
    private String description = "";   // 설명 한 줄 (글쓰기 · 피드 카테고리 탭)

    @Column(nullable = false)
    private int displayOrder;   // 보이는 순서 (작을수록 앞)

    public static final int WEEKLY_TOPIC_MAX_LENGTH = 100;

    @Column(length = WEEKLY_TOPIC_MAX_LENGTH)
    private String weeklyTopic;   // 이번 주 써 볼 주제 (없으면 null — 앱에서 안 보임)

    @Column(nullable = false)
    private boolean isActive;   // 사용여부

    // 빈 값 · 공백만은 주제 지움
    public void changeWeeklyTopic(String weeklyTopic) {
        this.weeklyTopic = weeklyTopic == null || weeklyTopic.isBlank() ? null : weeklyTopic.strip();
    }

    public void update(String name, String description, int displayOrder, String weeklyTopic, boolean isActive) {
        this.name = name;
        this.description = description == null ? "" : description;
        this.displayOrder = displayOrder;
        this.weeklyTopic = weeklyTopic == null || weeklyTopic.isBlank() ? null : weeklyTopic.strip();
        this.isActive = isActive;
    }
}
