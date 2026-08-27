package com.example.ssds.infra.port;

import com.example.ssds.core.port.ClimateNormalRepositoryPort;
import com.example.ssds.infra.entity.CategoryClimateProfile;
import com.example.ssds.infra.repository.CategoryClimateProfileRepository;
import com.example.ssds.infra.repository.ClimateNormalRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClimateNormalRepositoryPortAdapter implements ClimateNormalRepositoryPort {

    private final ClimateNormalRepository climateNormalRepository;
    private final CategoryClimateProfileRepository categoryClimateProfileRepository;

    public ClimateNormalRepositoryPortAdapter(
            ClimateNormalRepository climateNormalRepository,
            CategoryClimateProfileRepository categoryClimateProfileRepository) {
        this.climateNormalRepository = climateNormalRepository;
        this.categoryClimateProfileRepository = categoryClimateProfileRepository;
    }

    @Override
    public Optional<BigDecimal> findAvgTemp(String regionCode, int month) {
        return climateNormalRepository.findByRegionCodeAndMonth(regionCode, (short) month)
                .map(normal -> normal.getAvgTemp());
    }

    @Override
    public Optional<IdealTempRange> findCategoryIdealTempRange(long categoryId) {
        return categoryClimateProfileRepository.findById(categoryId)
                .map(this::toRange);
    }

    private IdealTempRange toRange(CategoryClimateProfile profile) {
        return new IdealTempRange(profile.getIdealTempMin(), profile.getIdealTempMax());
    }
}
