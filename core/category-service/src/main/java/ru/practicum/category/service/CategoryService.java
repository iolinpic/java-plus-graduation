package ru.practicum.category.service;

import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto saveCategory(NewCategoryDto dto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto dto);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategoryById(Long catId);

    List<CategoryDto> getCategoriesByIds(List<Long> ids);
}
