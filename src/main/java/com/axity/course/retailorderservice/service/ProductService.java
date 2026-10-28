package com.axity.course.retailorderservice.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axity.course.retailorderservice.dto.ProductRequestDto;
import com.axity.course.retailorderservice.dto.ProductResponseDto;
import com.axity.course.retailorderservice.event.StockChangedEvent;
import com.axity.course.retailorderservice.exception.EstadoInvalidoException;
import com.axity.course.retailorderservice.exception.PrecioInvalidoException;
import com.axity.course.retailorderservice.exception.ProductNotFoundException;
import com.axity.course.retailorderservice.model.Product;
import com.axity.course.retailorderservice.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository, ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> findAll(String category, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Product> spec = buildFilterSpecification(category, minPrice, maxPrice);
        return productRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto findById(Long id) {
        return toResponse(getActiveProduct(id));
    }

    public ProductResponseDto create(ProductRequestDto request) {
        validatePrice(request.price());
        Product product = new Product(
                request.sku(),
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.category());
        return toResponse(productRepository.save(product));
    }

    public ProductResponseDto update(Long id, ProductRequestDto request) {
        validatePrice(request.price());
        Product product = getActiveProduct(id);
        boolean stockChanged = !request.stock().equals(product.getStock());
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());
        ProductResponseDto response = toResponse(productRepository.save(product));
        if (stockChanged) {
            eventPublisher.publishEvent(new StockChangedEvent(product.getId()));
        }
        return response;
    }

    /**
     * Baja lógica: preserva el histórico (pedidos previos siguen referenciando el producto)
     * y evita que el catálogo lo siga ofreciendo. Desactivar un producto ya inactivo se trata
     * como "no encontrado", igual que cualquier otra operación sobre un producto dado de baja.
     */
    public void delete(Long id) {
        Product product = getActiveProduct(id);
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Revierte la baja lógica. Busca sin filtrar por active (a diferencia de getActiveProduct)
     * porque el objetivo es precisamente encontrar un producto inactivo. Reactivar un producto
     * que ya está activo se rechaza (409) en vez de tratarse como no-op silencioso, igual que
     * la cancelación duplicada de pedidos.
     */
    public ProductResponseDto reactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (product.isActive()) {
            throw new EstadoInvalidoException("El producto " + id + " ya se encuentra activo");
        }
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PrecioInvalidoException("El precio debe ser mayor que cero");
        }
    }

    private Product getActiveProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        if (!product.isActive()) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    private Specification<Product> buildFilterSpecification(String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isTrue(root.get("active")));
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("category")), category.toLowerCase()));
            }
            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ProductResponseDto toResponse(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.isActive());
    }
}
