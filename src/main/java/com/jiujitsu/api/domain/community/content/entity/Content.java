package com.jiujitsu.api.domain.community.content.entity;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "content")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType contentType;  // 컨텐츠 타입

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommunityComments> comments = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "content_image_file",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "image_file_id")
    )
    @Builder.Default
    private List<ImageFile> imageFiles = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContentLike> likes = new ArrayList<>();   // 좋아요

    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0;     // 조회수

    @Column(nullable = false)
    @Builder.Default
    private boolean authorViewCounted = false;  // 작성자 조회수 추가됐는지 여부

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementViewCountForAuthor() {
        if (!this.authorViewCounted) {
            this.viewCount++;
            this.authorViewCounted = true;
        }
    }

    public void addImageFiles(List<ImageFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return;
        }
        imageFiles.forEach(this::addImageFile);
    }

    public void addImageFile(ImageFile imageFile) {
        if (imageFile == null) {
            return;
        }
        imageFile.activate();
        this.imageFiles.add(imageFile);
    }
}