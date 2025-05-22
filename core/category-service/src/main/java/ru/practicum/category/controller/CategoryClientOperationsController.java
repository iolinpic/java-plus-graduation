package ru.practicum.category.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.category.service.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.CategoryClientOperations;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryClientOperationsController implements CategoryClientOperations {
    private final CategoryService categoryService;

    @Override
    public CategoryDto findById(@RequestParam Long id) throws FeignException {
        return categoryService.getCategoryById(id);
    }

    @Override
    @GetMapping("/list")
    public List<CategoryDto> findByIds(@RequestParam List<Long> ids) throws FeignException {
        return categoryService.getCategoriesByIds(ids);
    }
}
