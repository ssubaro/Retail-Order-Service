package com.axity.course.retailorderservice.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.axity.course.retailorderservice.dto.ProductRequestDto;
import com.axity.course.retailorderservice.dto.ProductResponseDto;
import com.axity.course.retailorderservice.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponseDto> findAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.findAll(category, minPrice, maxPrice);
    }

    @GetMapping("/{id}")
    public ProductResponseDto findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto request) {
        ProductResponseDto created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    public ProductResponseDto reactivate(@PathVariable Long id) {
        return productService.reactivate(id);
    }
}
