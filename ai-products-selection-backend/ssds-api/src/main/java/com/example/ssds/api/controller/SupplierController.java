package com.example.ssds.api.controller;

import com.example.ssds.api.common.response.ApiResponse;
import com.example.ssds.api.dto.SupplierOption;
import com.example.ssds.infra.repository.SupplierRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 供應商下拉選單，供 FR-03 品項表單使用。 */
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository supplierRepository;

    public SupplierController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @GetMapping
    public ApiResponse<List<SupplierOption>> listSuppliers(@RequestParam(required = false) String keyword) {
        var suppliers = keyword == null || keyword.isBlank()
                ? supplierRepository.findAll()
                : supplierRepository.findByNameContainingIgnoreCase(keyword);
        return ApiResponse.success(suppliers.stream()
                .map(s -> new SupplierOption(s.getId(), s.getName()))
                .toList());
    }
}
