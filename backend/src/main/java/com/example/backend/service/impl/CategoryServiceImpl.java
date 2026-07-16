package com.example.backend.service.impl;

import com.example.backend.dto.response.CategoryDTO;
import com.example.backend.entity.Category;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findByStatus(1).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        String slug = categoryDTO.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = categoryDTO.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        }

        Category category = Category.builder()
                .name(categoryDTO.getName())
                .slug(slug)
                .description(categoryDTO.getDescription())
                .status(1)
                .build();

        Category saved = categoryRepository.save(category);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục id: " + id));

        if (categoryDTO.getName() != null) category.setName(categoryDTO.getName());
        if (categoryDTO.getSlug() != null) category.setSlug(categoryDTO.getSlug());
        if (categoryDTO.getDescription() != null) category.setDescription(categoryDTO.getDescription());

        Category updated = categoryRepository.save(category);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteCategorySoft(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục!"));
        category.setStatus(0);
        categoryRepository.save(category);
    }

    private CategoryDTO mapToDTO(Category c) {
        return CategoryDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .build();
    }
}
