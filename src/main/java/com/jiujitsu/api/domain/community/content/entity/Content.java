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

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ImageFile> imageFiles = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContentLike> likes = new ArrayList<>();   // 좋아요

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
        imageFile.attachTo(this);
        this.imageFiles.add(imageFile);
    }
}