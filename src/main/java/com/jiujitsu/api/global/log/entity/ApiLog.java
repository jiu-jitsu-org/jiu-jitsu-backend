package com.jiujitsu.api.global.log.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Column(nullable = false)
    private Boolean success;

    @Column(nullable = false)
    private Integer statusCode;

    @Column
    private String errorType;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column(length = 10)
    private String method;

    @Column
    private String uri;
}
