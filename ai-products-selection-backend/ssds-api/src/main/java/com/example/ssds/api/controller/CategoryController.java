package com.example.ssds.api.controller;

import com.example.ssds.api.common.response.ApiResponse;
import com.example.ssds.api.dto.CategoryOption;
import com.example.ssds.infra.entity.Category;
import com.example.ssds.infra.repository.CategoryRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 品類下拉選單，供 FR-03 品項表單與清單篩選使用。 */
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ApiResponse<List<CategoryOption>> listCategories() {
        List<CategoryOption> options = categoryRepository.findTreeWithChildren().stream()
                .flatMap(parent -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(toOption(parent)),
                        parent.getChildren().stream().map(this::toOption)))
                .toList();
        return ApiResponse.success(options);
    }

    private CategoryOption toOption(Category c) {
        return new CategoryOption(c.getId(), c.getName(), c.getParent() == null ? null : c.getParent().getId());
    }
}
