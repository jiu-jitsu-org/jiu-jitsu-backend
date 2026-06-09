package com.jiujitsu.api.domain.user.controller;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "[SYS] CI/CD-test-controller", description = "Test CI/CD")
@CommonApiResponses
@RestController
@RequestMapping("/test")
public class DeployTestController {

    @Operation(
            summary = "Get a greeting message",
            description = "Returns a personalized greeting message"
    )
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER})
    @GetMapping("/greeting")
    public ResponseEntity<Map<String, String>> getGreeting(
            @Parameter(description = "Name of the person to greet", example = "John")
            @RequestParam(defaultValue = "World") String name) {

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, " + name + "!!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
