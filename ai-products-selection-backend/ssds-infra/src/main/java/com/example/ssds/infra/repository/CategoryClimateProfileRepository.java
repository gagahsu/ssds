package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.CategoryClimateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 品類層級預設適溫區間（規格書 §7.2 category_climate_profile、§FR-17-2）。 */
@Repository
public interface CategoryClimateProfileRepository
        extends JpaRepository<CategoryClimateProfile, Long> {
}
