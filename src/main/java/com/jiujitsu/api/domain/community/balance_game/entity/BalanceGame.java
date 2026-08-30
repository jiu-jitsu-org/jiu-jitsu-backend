package com.jiujitsu.api.domain.community.balance_game.entity;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "balance_game")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceGame extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글/좋아요/조회수 등 커뮤니티 공통 기능은 Content 를 재사용한다. (contentType = BALANCE)
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;

    @Column(nullable = false, length = 100)
    private String optionAText;         // 선택지 A 문구

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_a_image_id")
    private ImageFile optionAImage;     // 선택지 A 이미지 (선택)

    @Column(nullable = false, length = 100)
    private String optionBText;         // 선택지 B 문구

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_b_image_id")
    private ImageFile optionBImage;     // 선택지 B 이미지 (선택)

    @Column(nullable = false)
    private LocalDateTime endAt;        // 마감 일시 (프론트 카운트다운 기준)
}
