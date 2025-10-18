package com.jiujitsu.api.domain.community.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "owner_profile")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class OwnerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentImageUrl;    // 인증사진 url

    @Column
    private String teachingPhilosophy;    // 지도 철학

    @Column(nullable = false)
    private LocalDate teachingStartDate;    // 지도 경력시작일

    @Column
    private String teachingDetail;    // 경력 상세
}
