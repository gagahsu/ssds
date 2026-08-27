package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.CategoryAudienceMix;
import com.example.ssds.infra.entity.id.CategoryAudienceMixId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 品類客群組成（規格書 §7.2 category_audience_mix、§5.2.4）。 */
@Repository
public interface CategoryAudienceMixRepository
        extends JpaRepository<CategoryAudienceMix, CategoryAudienceMixId> {

    @EntityGraph(attributePaths = {"audience"})
    List<CategoryAudienceMix> findByCategoryId(Long categoryId);
}
