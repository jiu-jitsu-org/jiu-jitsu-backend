package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.reaction.CommunityCommentReactionSummary;
import com.jiujitsu.api.domain.community.comment.entity.CommunityCommentReaction;
import com.jiujitsu.api.domain.community.comment.entity.CommunityCommentReactionType;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentReactionRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityCommentsReactionService {

    private final CommunityCommentReactionRepository commentReactionRepository;
    private final CommunityCommentsRepository commentsRepository;

    @Transactional
    public CommunityCommentReactionSummary toggle(
            Long commentId,
            Long userId,
            CommunityCommentReactionType clickedType
    ) {
        CommunityComments comment = commentsRepository.getReferenceById(commentId);

        Optional<CommunityCommentReaction> existingOpt = commentReactionRepository.findByCommentAndUser(commentId, userId);

        if (existingOpt.isEmpty()) {
            // 처음누름 -> INSERT
            commentReactionRepository.save(
                    CommunityCommentReaction
                            .builder()
                            .comment(comment)
                            .reactionType(clickedType)
                            .build()
            );
        } else {
            CommunityCommentReaction existing = existingOpt.get();
            if (existing.getReactionType() == clickedType) {
                // 같은 버튼 다시 누름 -> DELETE
                commentReactionRepository.delete(existing);
                clickedType = null;
            } else {
                // 반대 버튼 누름 -> UPDATE
                existing.changeTo(clickedType);
            }
        }

        long likes = commentReactionRepository.countByCommentAndReactionType(commentId, CommunityCommentReactionType.LIKE.name());
        long disLikes = commentReactionRepository.countByCommentAndReactionType(commentId, CommunityCommentReactionType.DISLIKE.name());

        return new CommunityCommentReactionSummary(likes, disLikes, clickedType);
    }

}
