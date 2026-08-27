package com.example.ssds.infra.port;

import com.example.ssds.core.domain.FactorCode;
import com.example.ssds.core.domain.SceneType;
import com.example.ssds.core.port.WeightProfileRepositoryPort;
import com.example.ssds.infra.entity.WeightProfile;
import com.example.ssds.infra.repository.WeightProfileRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WeightProfileRepositoryPortAdapter implements WeightProfileRepositoryPort {

    private final WeightProfileRepository weightProfileRepository;

    public WeightProfileRepositoryPortAdapter(WeightProfileRepository weightProfileRepository) {
        this.weightProfileRepository = weightProfileRepository;
    }

    @Override
    public Map<FactorCode, BigDecimal> findWeights(long weightVersionId, SceneType sceneType) {
        Map<FactorCode, BigDecimal> weights = new EnumMap<>(FactorCode.class);
        for (WeightProfile profile
                : weightProfileRepository.findByVersionIdAndSceneType(weightVersionId, sceneType)) {
            weights.put(profile.getFactorCode(), profile.getWeight());
        }
        return weights;
    }
}
