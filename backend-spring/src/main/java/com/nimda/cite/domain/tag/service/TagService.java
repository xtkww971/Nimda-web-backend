package com.nimda.cite.domain.tag.service;


import com.nimda.cite.common.exception.errorcode.ErrorCode;
import com.nimda.cite.common.exception.NotFoundException;
import com.nimda.cite.domain.board.entity.Category;
import com.nimda.cite.domain.board.repository.CategoryRepository;
import com.nimda.cite.domain.tag.entity.Tag;
import com.nimda.cite.domain.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final CategoryRepository categoryRepository;

    private final TagRepository tagRepository;

    // 모든 태그 조회
    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderBySortValueAsc();
    }

    @Transactional
    public Tag addTag(String name, Integer value, Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
        );

        Tag tag = Tag.builder().tagName(name)
                .sortValue(value)
                .category(category)
                .build();
        tagRepository.save(tag);
        return tag;
    }

    // 카테고리 id로 태그 조회
    @Transactional(readOnly = true)
    public List<Tag> searchTag(Long categoryId) {
        return tagRepository.findAllByCategoryIdOrderBySortValueAsc(categoryId);
    }

    // 태그 삭제
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId).orElseThrow(
                () -> new NotFoundException(ErrorCode.TAG_NOT_FOUND)
        );

        tagRepository.deleteById(tagId);
    }

    @Transactional
    public Tag tagUpdate(Long tagId, String name, Integer sortValue, Long categoryId) {
        Tag tag = tagRepository.findById(tagId).orElseThrow(
                () -> new NotFoundException(ErrorCode.TAG_NOT_FOUND)
        );

        if (name != null && !name.isBlank()) tag.setTagName(name);
        if (sortValue != null) tag.setSortValue(sortValue);


        if (categoryId != null) {
            // 1. 현재 태그에 카테고리가 없거나(null)
            // 2. 있는데 가져온 ID와 다를 때만 실행
            if (tag.getCategory() == null || !tag.getCategory().getId().equals(categoryId)) {
                Category category = categoryRepository.findById(categoryId).orElseThrow(
                        () -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
                );
                tag.setCategory(category);
            }
        }
        tagRepository.save(tag);

        return tag;
    }
}
