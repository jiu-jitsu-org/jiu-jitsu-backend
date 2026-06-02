package com.jiujitsu.api.domain;

import com.jiujitsu.api.domain.community.board.service.BoardCategoryService;
import com.jiujitsu.api.domain.community.board.service.BoardService;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.service.AuthService;
import com.jiujitsu.api.domain.user.service.UserService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "test-controller", description = "---테스트용 endpoints---")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final UserService userService;
    private final BoardCategoryService boardCategoryService;
    private final AuthService authService;

    /**
     * 인증
     */
    @Operation(summary = "앱 테스트용 로그인(닉네임)")
    @PostMapping("/test-login")
    public AuthResponse testLogin(@Valid @RequestBody SnsLoginRequest request) { return authService.testLogin(request); }

    @Operation(
            summary = "(관리자) 로그인",
            description = "관리자 로그인 테스트"
    )
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return new LoginResponse();
    }

    /**
     * 유저
     */
    @Operation(
            summary = "(TEST) 회원 앱 정보 조회",
            description = "로그인 회원의 앱 정보 조회합니다."
    )
    @GetMapping("/appInfo")
    public List<UserAppInfoResponse> getUserAppInfo() {
        return userService.getUserAppInfo();
    }

    @Operation(
            summary = "(TEST) 닉네임 기반 회원 앱 정보 조회",
            description = "닉네임 검색으로 일치하는 회원의 앱 정보 조회합니다."
    )
    @GetMapping("/appInfo/nickname")
    public List<UserAppInfoResponse> getUserAppInfoByNickname(@RequestParam(value = "nickname") String nickname) {
        return userService.getUserAppInfoByNickname(nickname);
    }

    @Operation(
            summary = "(관리자로 옮길거임) 관장/사범 권한 부여",
            description = "사용자에게 관장/사범 권한을 부여합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @PutMapping("/grant/owner-role")
    public UserProfileResponse grantOwnerRole() {
        return userService.grantOwnerRole();
    }

    /**
     * 커뮤니티
     */
    //todo: 카테고리 생성 > 2차 오픈 시 관리자 메뉴로 이동
    @Operation(summary = "카테고리 생성(임시)", description = "카테고리 생성(임시)")
    @PostMapping("/category")
    public void createCategory() {
        boardCategoryService.createCategory();
    }
}
