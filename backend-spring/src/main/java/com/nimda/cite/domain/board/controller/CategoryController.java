package com.nimda.cite.domain.board.controller;

import com.nimda.cite.domain.board.dto.CategoryCreateDTO;
import com.nimda.cite.domain.board.dto.CategoryResponseDTO;
import com.nimda.cite.domain.board.dto.CategorySortOrderDTO;
import com.nimda.cite.domain.board.dto.CategoryUpdateDTO;
import com.nimda.cite.domain.board.entity.Category;
import com.nimda.cite.domain.board.service.CategoryService;
import com.nimda.cite.common.response.ApiResponse;
import com.nimda.cite.common.util.UrlNames;
import com.nimda.cite.user.entity.User;
import com.nimda.cite.user.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(UrlNames.categoryUrl)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // API1. getAllCategories
    // feat. 활성화된 카테고리 조회 API (일반 사용자용)
    @GetMapping(UrlNames.getAllCategoriesUrl)
    public ResponseEntity<?> getAllCategories() {
        List<Category> categories = categoryService.getAllActiveCategories();
        List<CategoryResponseDTO> categoryDTOList = categories.stream()
                .map(CategoryResponseDTO::from)
                .toList();
        return ApiResponse.ok(categoryDTOList).toResponse();
    }

    /**
     * API1-1. getAllCategoriesAdmin
     * feat. 모든 카테고리 조회 API (관리자용, isActive 여부 관계없이)
     * - 관리자만 접근 가능
     * - 활성화/비활성화 모든 카테고리 반환
     */
    @GetMapping(UrlNames.getAllCategoriesAdminUrl)
    public ResponseEntity<?> getAllCategoriesAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User user = CustomUserDetails.getLoginUser(userDetails);

        List<Category> categories = categoryService.getAllCategories(user);
        List<CategoryResponseDTO> categoryDTOList = categories.stream()
                .map(CategoryResponseDTO::from)
                .toList();
        return ApiResponse.ok(categoryDTOList).toResponse();
    }

    // API1-2 getCategoryBySlug
    // feat. 카테고리명(slug)로 조회 API
    @GetMapping(UrlNames.getCategoryBySlugUrl)
    public ResponseEntity<?> getCategoryBySlug(@PathVariable String slug) {
        Category category = categoryService.getCategoryBySlug(slug);
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.from(category);
        return ApiResponse.ok(categoryDTO).toResponse();
    }

    /**
     * API2. createCategoryAPI
     * feat. 카테고리 생성 API
     * - 관리자만 카테고리 생성 가능
     * - JWT 토큰에서 사용자 정보 추출 후 권한 확인
     */
    @PostMapping(UrlNames.createCategoryUrl)
    public ResponseEntity<?> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CategoryCreateDTO createDTO) {
        User user = CustomUserDetails.getLoginUser(userDetails);

        Category category = categoryService.createCategory(createDTO, user);
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.from(category);
        return ApiResponse.ok(categoryDTO).toResponse(HttpStatus.CREATED);
    }

    /**
     * API3. updateCategoryAPI
     * feat. 카테고리 수정 API
     * - 관리자만 카테고리 수정 가능
     * - JWT 토큰에서 사용자 정보 추출 후 권한 확인
     */
    @PutMapping(UrlNames.updateCategoryUrl)
    public ResponseEntity<?> updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateDTO updateDTO) {
        User user = CustomUserDetails.getLoginUser(userDetails);

        Category category = categoryService.updateCategory(id, updateDTO, user);
        CategoryResponseDTO categoryDTO = CategoryResponseDTO.from(category);
        return ApiResponse.ok(categoryDTO).toResponse();
    }

    /**
     * API4. deleteCategory
     * feat. 카테고리 삭제 API
     * - 관리자만 카테고리 삭제 가능
     * - JWT 토큰에서 사용자 정보 추출 후 권한 확인
     * - 소프트 삭제 (isActive = false)
     */
    @DeleteMapping(UrlNames.deleteCategoryUrl)
    public ResponseEntity<?> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        User user = CustomUserDetails.getLoginUser(userDetails);

        categoryService.deleteCategory(id, user);
        return ApiResponse.ok("카테고리가 삭제되었습니다.").toResponse();
    }

    /**
     * API5. updateSortOrders
     * feat. 카테고리 순서 일괄 업데이트 API
     * - 관리자만  수 있음
     */
    @PutMapping(UrlNames.updateSortOrdersUrl)
    public ResponseEntity<?> updateSortOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<CategorySortOrderDTO> sortOrders) {
        User user = CustomUserDetails.getLoginUser(userDetails);

        categoryService.updateSortOrders(sortOrders, user);
        return ApiResponse.ok("카테고리 순서가 업데이트되었습니다.").toResponse();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(UrlNames.enableShopUrl)
    public ResponseEntity<?> enableShop(@PathVariable long id) {
        Category category = categoryService.setShopEnabled(id);

        CategoryUpdateDTO dto = CategoryUpdateDTO.from(category);
        return ApiResponse.ok(dto).toResponse();
    }
}

