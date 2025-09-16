package com.jiujitsu.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sample")
@Tag(name = "Sample", description = "Sample API endpoints for demonstration")
public class SampleController {

    @Operation(
            summary = "Get a greeting message",
            description = "Returns a personalized greeting message"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved greeting",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid name parameter",
                    content = @Content)
    })
    @GetMapping("/greeting")
    public ResponseEntity<Map<String, String>> getGreeting(
            @Parameter(description = "Name of the person to greet", example = "John")
            @RequestParam(defaultValue = "World") String name) {
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, " + name + "!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Create a new item",
            description = "Creates a new item with the provided data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Item created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content)
    })
    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> createItem(
            @Parameter(description = "Item data", required = true)
            @RequestBody Map<String, Object> itemData) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", java.util.UUID.randomUUID().toString());
        response.put("data", itemData);
        response.put("created_at", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.status(201).body(response);
    }

    @Operation(
            summary = "Get item by ID",
            description = "Retrieves an item by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content)
    })
    @GetMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> getItem(
            @Parameter(description = "Unique identifier of the item", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("name", "Sample Item");
        response.put("description", "This is a sample item");
        response.put("created_at", java.time.LocalDateTime.now().minusDays(1).toString());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get a greeting message",
            description = "Returns a personalized greeting message"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved greeting",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Invalid name parameter",
                    content = @Content)
    })
    @GetMapping("/chanq")
    public ResponseEntity<Map<String, String>> getChanq(
            @Parameter(description = "Name of the person to greet", example = "John")
            @RequestParam(defaultValue = "World") String name) {

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, ChanQ TEST!!!!!! " + name + "!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
