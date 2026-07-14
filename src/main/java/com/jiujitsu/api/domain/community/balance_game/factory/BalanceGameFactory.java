package com.jiujitsu.api.domain.community.balance_game.factory;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BalanceGameFactory {

    private final ImageFileRepository imageFileRepository;

    public Content createContent() {
        return Content.builder()
                .contentType(ContentType.BALANCE)
                .build();
    }

    public BalanceGame createBalanceGame(Content content,
                                         String optionAText, Long optionAImageId,
                                         String optionBText, Long optionBImageId,
                                         LocalDateTime endAt) {
        return BalanceGame.builder()
                .content(content)
                .optionAText(optionAText)
                .optionAImage(findActiveImageOrNull(optionAImageId))
                .optionBText(optionBText)
                .optionBImage(findActiveImageOrNull(optionBImageId))
                .endAt(endAt)
                .build();
    }

    // 이미지 ID가 있으면 조회 후 ACTIVE 처리(임시 이미지 정리 대상에서 제외), 없으면 null
    private ImageFile findActiveImageOrNull(Long imageFileId) {
        if (imageFileId == null) {
            return null;
        }
        ImageFile imageFile = imageFileRepository.findById(imageFileId)
                .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND));
        imageFile.activate();
        return imageFile;
    }
}
