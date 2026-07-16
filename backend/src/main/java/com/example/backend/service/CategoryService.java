package com.example.backend.service;

import com.example.backend.dto.response.CategoryDTO;
import java.util.List;

public interface CategoryService {

    List<CategoryDTO> getAllCategories();

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO);

    void deleteCategorySoft(Long id);
}
