package com.jiujitsu.api.domain.file;

import com.jiujitsu.api.domain.community.content.entity.Content;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_url")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUrl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(nullable = false, length = 1024)
    private String imageUrl;

    public void attachTo(Content content) {
        this.content = content;
    }
}
