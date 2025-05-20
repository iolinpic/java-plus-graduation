package ru.practicum.category.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.category.service.CategoryService;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.category.CategoryClient;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryClientController implements CategoryClient {
    private final CategoryService categoryService;

    @Override
    public CategoryDto findById(Long id) throws FeignException {
        return categoryService.getCategoryById(id);
    }

    @Override
    public List<CategoryDto> findByIds(List<Long> ids) throws FeignException {
        return categoryService.getCategoriesByIds(ids);
    }
}
