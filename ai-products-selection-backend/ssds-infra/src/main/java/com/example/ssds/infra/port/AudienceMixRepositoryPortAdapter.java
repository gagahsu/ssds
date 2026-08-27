package com.example.ssds.infra.port;

import com.example.ssds.core.port.AudienceMixRepositoryPort;
import com.example.ssds.core.scoring.AudienceSegmentShare;
import com.example.ssds.infra.repository.CategoryAudienceMixRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AudienceMixRepositoryPortAdapter implements AudienceMixRepositoryPort {

    private final CategoryAudienceMixRepository categoryAudienceMixRepository;

    public AudienceMixRepositoryPortAdapter(CategoryAudienceMixRepository categoryAudienceMixRepository) {
        this.categoryAudienceMixRepository = categoryAudienceMixRepository;
    }

    @Override
    public List<AudienceSegmentShare> findMixForCategory(long categoryId) {
        return categoryAudienceMixRepository.findByCategoryId(categoryId).stream()
                .map(mix -> new AudienceSegmentShare(
                        mix.getAudience().getAudienceCode(),
                        mix.getAudience().getPriceMin(),
                        mix.getAudience().getPriceMax(),
                        mix.getShare()))
                .toList();
    }
}
