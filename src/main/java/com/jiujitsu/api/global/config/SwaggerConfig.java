package com.jiujitsu.api.global.config;

import com.jiujitsu.api.global.exception.ApiResponse;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {
    @Value("${springdoc.swagger-ui.server.url}")
    private String swaggerServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("주짓수 커뮤니티 API")
                        .description("주짓수 커뮤니티 API 명세")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url(swaggerServerUrl)
                                .description("주짓수 server")
                ))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    @Bean
    public OperationCustomizer customize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            List<ErrorCode> allErrorCodes = new ArrayList<>();

            // 메서드 annotation
            ApiErrorCodeExamples methodExamples = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
            ApiErrorCodeExample methodExample = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);

            if (methodExamples != null) {
                allErrorCodes.addAll(Arrays.asList(methodExamples.value()));
            } else if (methodExample != null) {
                allErrorCodes.add(methodExample.value());
            }

            // 클래스 annotation
            ApiErrorCodeExamples classExamples = handlerMethod.getBeanType().getAnnotation(ApiErrorCodeExamples.class);
            ApiErrorCodeExample classExample = handlerMethod.getBeanType().getAnnotation(ApiErrorCodeExample.class);

            if (classExamples != null) {
                allErrorCodes.addAll(Arrays.asList(classExamples.value()));
            } else if (classExample != null) {
                allErrorCodes.add(classExample.value());
            }

            if (!allErrorCodes.isEmpty()) {
                generateErrorCodeResponseExample(operation, allErrorCodes.toArray(new ErrorCode[0]));
            }

            return operation;
        };
    }

    // ErrorCode 여러 개 응답값
    private void generateErrorCodeResponseExample(Operation operation, ErrorCode[] errorCodes) {
        ApiResponses responses = operation.getResponses();

        // 에러 응답값(ExampleHolder) 객체 만든 후 에러 코드별로 그룹화
        Map<Integer, List<ExampleHolder>> statusWithExampleHolders = Arrays.stream(errorCodes)
                .map(
                        errorCode -> ExampleHolder.builder()
                                .holder(getSwaggerExample(errorCode))
                                .code(errorCode.getStatus())
                                .name(errorCode.name())
                                .build()
                ).collect(Collectors.groupingBy(ExampleHolder::getCode));

        addExampleToResponses(responses, statusWithExampleHolders);
    }

    // ErrorCode 한 개 응답값
    private void generateErrorCodeResponseExample(Operation operation, ErrorCode errorCode) {
        ApiResponses responses = operation.getResponses();

        // ExampleHolder 객체 생성 및 ApiResponses에 추가
        ExampleHolder exampleHolder = ExampleHolder.builder()
                .holder(getSwaggerExample(errorCode))
                .name(errorCode.name())
                .code(errorCode.getStatus())
                .build();

        addExamplesToResponses(responses, exampleHolder);
    }

    private Example getSwaggerExample(ErrorCode errorCode) {
        ApiResponse<Object> apiResponse = new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
        Example example = new Example();
        example.setValue(apiResponse);

        return example;
    }

    private void addExampleToResponses(ApiResponses responses,
                                       Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
        statusWithExampleHolders.forEach(
                (status, v) -> {
                    Content content = new Content();
                    MediaType mediaType = new MediaType();
                    io.swagger.v3.oas.models.responses.ApiResponse apiResponse = new io.swagger.v3.oas.models.responses.ApiResponse();

                    v.forEach(
                            exampleHolder -> mediaType.addExamples(
                                    exampleHolder.getName(),
                                    exampleHolder.getHolder()
                            )
                    );
                    content.addMediaType("application/json", mediaType);
                    apiResponse.setContent(content);
                    responses.addApiResponse(String.valueOf(status), apiResponse);
                }
        );
    }

    private void addExamplesToResponses(ApiResponses responses, ExampleHolder exampleHolder) {
        Content content = new Content();
        MediaType mediaType = new MediaType();
        io.swagger.v3.oas.models.responses.ApiResponse apiResponse = new io.swagger.v3.oas.models.responses.ApiResponse();

        mediaType.addExamples(exampleHolder.getName(), exampleHolder.getHolder());
        content.addMediaType("application/json", mediaType);
        apiResponse.setContent(content);
        responses.addApiResponse(String.valueOf(exampleHolder.getCode()), apiResponse);
    }
}
