package com.jiujitsu.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * Bad Request
     */
    REQUIRED_PARAMETER(400, "R0001", "필수값이 입력되지 않았습니다. 다시 확인해 주세요."),
    WRONG_PARAMETER(400, "R0002", "잘못된 요청 데이터입니다. 다시 확인해 주세요."),
    NICKNAME_DUPLICATED(400, "R0003", "이미 사용중인 닉네임입니다."),
    NICKNAME_VALIDATION(400, "R0004", "잘못된 닉네임 형식입니다."),
    REQUIRED_PROFILE(400, "R0005", "프로필 등록 후 처리 가능합니다."),
    IMAGE_FILE_NOT_FOUND(404, "C0000", "존재하지 않는 이미지 파일입니다."),
    CONTENT_NOT_FOUND(404, "C0001", "존재하지 않는 게시물입니다."),
    BOARD_NOT_FOUND(404, "C0002", "존재하지 않는 게시글입니다."),
    BOARD_CATEGORY_NOT_FOUND(404, "C0003", "존재하지 않는 카테고리입니다."),
    COMMENT_NOT_FOUND(404, "C0004", "존재하지 않는 댓글입니다."),
    NOTICE_NOT_FOUND(404, "C0004", "존재하지 않는 게시물입니다."),
    EMAIL_DUPLICATED(400, "R0006", "이미 사용중인 이메일입니다."),


    /**
     * 응답 에러
     */
    ENTITY_NOT_FOUND(404, "P0001", "데이터가 존재하지 않습니다."),
    FAILED_SIGNATURE(500, "P0002", "서명 생성 중 오류가 발생하였습니다."),
    FAILED_CDN_DELETE(500, "P0003", "CDN 이미지 삭제 중 오류가 발생하였습니다."),


    /**
     * 유저 에러
     */
    LOGIN_NOT_ACCESS(403, "U0001", "로그인이 필요한 서비스입니다. 로그인을 해주세요."),
    USER_NOT_FOUND(401, "U0002", "존재하지 않는 유저입니다."),
    PERMISSION_DENIED(403, "U0003", "권한이 없습니다."),
    USER_ALREADY_DEACTIVATED(400, "U0004", "이미 탈퇴 처리된 계정입니다."),
    USER_ACCOUNT_EXPIRED(400, "U0005", "탈퇴 후 30일이 지나 계정이 영구 삭제되었습니다."),
    SELF_BLOCK_NOT_ALLOWED(400, "U0006", "자기 자신을 차단할 수 없습니다."),

    /**
     * 인증 에러
     */
    NO_TOKEN(400, "A0001", "토큰이 존재하지 않습니다."),
    NOT_MATCH_CATEGORY(400, "A0002", "잘못된 유형의 토큰입니다."),
    TOKEN_EXPIRED(403, "A0003", "만료된 토큰입니다."),
    ACCESS_TOKEN_EXPIRED(403, "A004", "만료된 엑세스 토큰입니다."),
    TOKEN_NOT_EXPIRED(403, "A0005", "아직 토큰이 만료되지 않았습니다."),
    INVALID_TOKEN(401, "A0006", "유효하지 않은 토큰입니다."),
    UNAUTHENTICATED_USER(401, "A0007", "인증정보가 등록되지 않았습니다. 서버에 문의해 주세요."),
    FAIL_CREATE_REVOKE_TOKEN(500, "A0008", "revoke Token 생성에 실패했습니다."),
    INVALID_REFRESH_TOKEN(401, "A0009", "유효하지 않은 refresh token입니다. 재로그인 해 주세요."),
    KEY_PARSING_ERROR(401, "A0010", "인증 키 파싱 중 오류가 발생하였습니다."),
    AUTHENTICATION_FAILED(401, "A0010", "인증에 실패하였습니다."),

    /**
     * 앱 버전 조회 에러
     */
    NO_APP_VERSION_DATA(400, "V0001", "데이터가 존재하지 않습니다."),

    /**
     * 서버 에러
     */
    INVALID_DATA_FORMAT(400, "SY001", "잘못된 요청 데이터입니다."),
    SERVER_ERROR(500, "SY002", "알 수 없는 에러가 발생했습니다. 서버에 문의해 주세요.");

    private final int status;
    private final String code;
    private final String message;
}
