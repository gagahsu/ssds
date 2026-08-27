package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.HeatCompositeDaily;
import com.example.ssds.infra.entity.id.HeatCompositeDailyId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 多來源合成後的每日熱度（規格書 §7.2 heat_composite_daily、§5.3.2）。 */
@Repository
public interface HeatCompositeDailyRepository
        extends JpaRepository<HeatCompositeDaily, HeatCompositeDailyId> {

    Optional<HeatCompositeDaily> findByKeywordIdAndStatDate(Long keywordId, LocalDate statDate);

    /** §5.3.3 斜率計算需要的歷史區間；由舊到新排序方便取「現有最長區間」。 */
    List<HeatCompositeDaily> findByKeywordIdAndStatDateBetweenOrderByStatDateAsc(
            Long keywordId, LocalDate from, LocalDate to);
}
