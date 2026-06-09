package com.jiujitsu.api.global.config;

import com.jiujitsu.api.global.exception.ApiResponse;
import com.jiujitsu.api.global.exception.ErrorCode;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;
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
        return (operation, handlerMethod) -> {

            Set<ErrorCode> allErrorCodes = new LinkedHashSet<>();

            collectErrorCodes(handlerMethod.getBeanType(), allErrorCodes);
            collectErrorCodes(handlerMethod.getMethod(), allErrorCodes);

            if (!allErrorCodes.isEmpty()) {
                generateErrorCodeResponseExample(
                        operation,
                        allErrorCodes.toArray(new ErrorCode[0])
                );
            }

            return operation;
        };
    }

    private void collectErrorCodes(
            AnnotatedElement element,
            Set<ErrorCode> allErrorCodes
    ) {

        // 직접 붙은 @ApiErrorCodeExamples
        ApiErrorCodeExamples direct =
                element.getAnnotation(ApiErrorCodeExamples.class);

        if (direct != null) {
            allErrorCodes.addAll(Arrays.asList(direct.value()));
        }

        // 커스텀 어노테이션들 검사
        for (Annotation annotation : element.getAnnotations()) {

            ApiErrorCodeExamples meta =
                    annotation.annotationType()
                            .getAnnotation(ApiErrorCodeExamples.class);

            if (meta != null) {
                allErrorCodes.addAll(Arrays.asList(meta.value()));
            }
        }
    }

//    @Bean
//    public OperationCustomizer customize() {
//        return (operation, handlerMethod) -> {
//
//            Set<ErrorCode> allErrorCodes = new LinkedHashSet<>();
//
//            // 메서드 어노테이션
//            collectErrorCodes(
//                    handlerMethod.getMethod(),
//                    allErrorCodes
//            );
//
//            // 클래스 어노테이션
//            collectErrorCodes(
//                    handlerMethod.getBeanType(),
//                    allErrorCodes
//            );
//
//            if (!allErrorCodes.isEmpty()) {
//                generateErrorCodeResponseExample(
//                        operation,
//                        allErrorCodes.toArray(new ErrorCode[0])
//                );
//            }
//
//            return operation;
//        };
//    }
//
//    private void collectErrorCodes(
//            AnnotatedElement element,
//            Set<ErrorCode> allErrorCodes
//    ) {
//
//        ApiErrorCodeExamples examples =
//                AnnotatedElementUtils.findMergedAnnotation(
//                        element,
//                        ApiErrorCodeExamples.class
//                );
//
//        if (examples != null) {
//            allErrorCodes.addAll(Arrays.asList(examples.value()));
//        }
//
//        ApiErrorCodeExample example =
//                AnnotatedElementUtils.findMergedAnnotation(
//                        element,
//                        ApiErrorCodeExample.class
//                );
//
//        if (example != null) {
//            allErrorCodes.add(example.value());
//        }
//    }

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
