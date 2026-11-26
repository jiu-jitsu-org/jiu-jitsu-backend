-- #241 탈퇴 30일 영구 삭제 FK 제약 수동 적용
-- ddl-auto: update 는 기존 FK 의 ON DELETE 를 바꾸지 않는다.
-- 아래 DDL 을 dev · prod DB 에 직접 실행해야 한다.
-- 실행 전 현재 제약 이름을 조회한 뒤 DROP → ADD 순으로 적용한다.

-- ① user_notice_setting.user_id → ON DELETE CASCADE
-- (가입 때 모든 회원에게 행이 생기므로 이 FK 가 CASCADE 가 아니면 거의 모든 삭제 실패)
ALTER TABLE user_notice_setting
    DROP FOREIGN KEY <fk_user_notice_setting_user_id>,   -- 실제 제약 이름으로 교체
    ADD CONSTRAINT fk_user_notice_setting_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ② user_block.blocker_id · blocked_id → ON DELETE CASCADE
ALTER TABLE user_block
    DROP FOREIGN KEY <fk_user_block_blocker_id>,
    ADD CONSTRAINT fk_user_block_blocker_id
        FOREIGN KEY (blocker_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_block
    DROP FOREIGN KEY <fk_user_block_blocked_id>,
    ADD CONSTRAINT fk_user_block_blocked_id
        FOREIGN KEY (blocked_id) REFERENCES users(id) ON DELETE CASCADE;

-- ③ BaseEntity 상속 테이블의 created_by · updated_by → ON DELETE SET NULL
-- 해당 테이블 목록 (content, board, comment, comment_like, content_like, board_save,
--   board_hide, report, balance_game_vote, competition_interest, ... 등 BaseEntity 상속 전부)
-- 각 테이블 FK 이름을 조회 후 동일한 패턴으로 DROP → ADD.
-- 예시 (content 테이블):
ALTER TABLE content
    DROP FOREIGN KEY <fk_content_created_by>,
    ADD CONSTRAINT fk_content_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- 현재 FK 이름 조회 쿼리:
-- SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME
-- FROM information_schema.KEY_COLUMN_USAGE
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND REFERENCED_TABLE_NAME = 'users';
