package ru.practicum.category.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exceptions.NotEmptyException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.EventClientOperations;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;
    private final EventClientOperations eventClient;

    @Override
    @Transactional
    public CategoryDto saveCategory(NewCategoryDto dto) {
        Category category = mapper.toCategory(dto);
        category = categoryRepository.save(category);
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        if (categoryHasEvents(catId)) {
            throw new NotEmptyException("Category with id=" + catId + " is not empty");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category categoryToUpdate = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        categoryToUpdate.setName(dto.getName());
        categoryRepository.save(categoryToUpdate);
        return mapper.toDto(categoryToUpdate);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        PageRequest page = PageRequest.of(from, size);
        return categoryRepository.findAll(page).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
        return mapper.toDto(category);
    }

    @Override
    public List<CategoryDto> getCategoriesByIds(List<Long> ids) {
        return categoryRepository.findAllById(ids).stream().map(mapper::toDto).toList();
    }

    private Boolean categoryHasEvents(Long catId) {
        try {
            return eventClient.categoryHasEvents(catId);
        } catch (FeignException e) {
            throw new NotFoundException("Events with category check failed");
        }
    }
}
