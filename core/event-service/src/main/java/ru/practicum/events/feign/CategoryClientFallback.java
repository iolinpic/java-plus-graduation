package ru.practicum.events.feign;

import feign.FeignException;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public CategoryDto findById(Long id) throws FeignException {
        return createCategoryDtoWithId(id);
    }

    @Override
    public List<CategoryDto> findByIds(List<Long> ids) throws FeignException {
        return ids.stream().map(this::createCategoryDtoWithId).toList();
    }

    private CategoryDto createCategoryDtoWithId(Long id) {
        CategoryDto cat = new CategoryDto();
        cat.setId(id);
        return cat;
    }
}
