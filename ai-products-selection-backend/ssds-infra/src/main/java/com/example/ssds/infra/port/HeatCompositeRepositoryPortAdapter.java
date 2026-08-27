package com.example.ssds.infra.port;

import com.example.ssds.core.port.HeatCompositeRepositoryPort;
import com.example.ssds.core.scoring.HeatSourceContribution;
import com.example.ssds.infra.entity.HeatReading;
import com.example.ssds.infra.repository.HeatCompositeDailyRepository;
import com.example.ssds.infra.repository.HeatReadingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HeatCompositeRepositoryPortAdapter implements HeatCompositeRepositoryPort {

    private final HeatReadingRepository heatReadingRepository;
    private final HeatCompositeDailyRepository heatCompositeDailyRepository;

    public HeatCompositeRepositoryPortAdapter(
            HeatReadingRepository heatReadingRepository,
            HeatCompositeDailyRepository heatCompositeDailyRepository) {
        this.heatReadingRepository = heatReadingRepository;
        this.heatCompositeDailyRepository = heatCompositeDailyRepository;
    }

    @Override
    public List<HeatSourceContribution> findSourceContributions(long keywordId, LocalDate statDate) {
        return heatReadingRepository.findByKeywordIdAndReadingDate(keywordId, statDate).stream()
                .filter(reading -> reading.getPercentileWithinSource() != null)
                .map(this::toContribution)
                .toList();
    }

    @Override
    public Optional<BigDecimal> findCompositeValue(long keywordId, LocalDate statDate) {
        return heatCompositeDailyRepository.findByKeywordIdAndStatDate(keywordId, statDate)
                .map(daily -> daily.getCompositeValue());
    }

    private HeatSourceContribution toContribution(HeatReading reading) {
        return new HeatSourceContribution(
                reading.getSource().getSourceCode().name(),
                reading.getSource().getCompositeWeight(),
                reading.getPercentileWithinSource(),
                reading.getSource().getAvailability(),
                reading.getSource().getGranularity());
    }
}
