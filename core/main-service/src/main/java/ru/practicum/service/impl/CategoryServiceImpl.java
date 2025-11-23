package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.entity.Category;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        if (newCategoryDto.getName() == null || newCategoryDto.getName().isBlank()) {
            throw new BadRequestException("Название категории не может быть пустым");
        } else if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с таким названием уже существует");
        }
        Category category = categoryMapper.toCategory(newCategoryDto);
        categoryValidator(category);
        categoryRepository.save(category);
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public void deleteCategory(Long catId) {
        categoryRepository.deleteById(catId);
    }

    @Override
    public CategoryDto updateCategory(Long catId, NewCategoryDto newCategoryDto) {
        Category category = categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException("Категория не найдена"));
        if (category.getName().equals(newCategoryDto.getName())) {
            return categoryMapper.toCategoryDto(category);
        }
        if (newCategoryDto.getName() != null) {
            category.setName(newCategoryDto.getName());
        }
        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с таким названием уже существует");
        }
        categoryValidator(category);
        categoryRepository.save(category);
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        return categoryRepository.findAll(PageRequest.of(from, size)).stream().map(categoryMapper::toCategoryDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException("Категория не найдена"));
        return categoryMapper.toCategoryDto(category);
    }

    private void categoryValidator(Category category) {
        if (category.getName().isBlank()) {
            throw new BadRequestException("Название категории не может быть пустым");
        } else if (category.getName().length() > 50) {
            throw new BadRequestException("Длина названия категории не может быть больше 50 символов");
        }
    }
}
