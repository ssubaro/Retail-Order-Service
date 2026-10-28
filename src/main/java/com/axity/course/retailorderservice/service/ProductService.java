package com.axity.course.retailorderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.course.retailorderservice.dto.ProductRequestDto;
import com.axity.course.retailorderservice.dto.ProductResponseDto;
import com.axity.course.retailorderservice.exception.ProductNotFoundException;
import com.axity.course.retailorderservice.model.Product;
import com.axity.course.retailorderservice.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto findById(Long id) {
        return toResponse(getProduct(id));
    }

    public ProductResponseDto create(ProductRequestDto request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.category());
        return toResponse(productRepository.save(product));
    }

    public ProductResponseDto update(Long id, ProductRequestDto request) {
        Product product = getProduct(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        return toResponse(productRepository.save(product));
    }

    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponseDto toResponse(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory());
    }
}
