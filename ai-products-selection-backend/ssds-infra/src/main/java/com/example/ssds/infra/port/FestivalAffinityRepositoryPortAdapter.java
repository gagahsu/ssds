package com.example.ssds.infra.port;

import com.example.ssds.core.port.FestivalAffinityRepositoryPort;
import com.example.ssds.core.scoring.FestivalAffinityInput;
import com.example.ssds.infra.entity.FestivalCalendar;
import com.example.ssds.infra.entity.ItemFestivalAffinity;
import com.example.ssds.infra.repository.CategoryLeadTimeRepository;
import com.example.ssds.infra.repository.FestivalCalendarRepository;
import com.example.ssds.infra.repository.ItemFestivalAffinityRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FestivalAffinityRepositoryPortAdapter implements FestivalAffinityRepositoryPort {

    private final ItemFestivalAffinityRepository itemFestivalAffinityRepository;
    private final FestivalCalendarRepository festivalCalendarRepository;
    private final CategoryLeadTimeRepository categoryLeadTimeRepository;

    public FestivalAffinityRepositoryPortAdapter(
            ItemFestivalAffinityRepository itemFestivalAffinityRepository,
            FestivalCalendarRepository festivalCalendarRepository,
            CategoryLeadTimeRepository categoryLeadTimeRepository) {
        this.itemFestivalAffinityRepository = itemFestivalAffinityRepository;
        this.festivalCalendarRepository = festivalCalendarRepository;
        this.categoryLeadTimeRepository = categoryLeadTimeRepository;
    }

    @Override
    public List<FestivalAffinityInput> findAffinities(long productId, int year) {
        List<ItemFestivalAffinity> affinities = itemFestivalAffinityRepository.findByProductId(productId);
        return affinities.stream()
                .flatMap(affinity -> festivalCalendarRepository
                        .findByFestivalCodeAndYear(affinity.getFestivalCode(), year)
                        .map(calendar -> toInput(affinity, calendar))
                        .stream())
                .toList();
    }

    @Override
    public int findLeadTimeDays(long categoryId) {
        return categoryLeadTimeRepository.findById(categoryId)
                .map(leadTime -> leadTime.getLeadTimeDays())
                .orElseThrow(() -> new IllegalStateException("找不到品類前置天數: categoryId=" + categoryId));
    }

    private FestivalAffinityInput toInput(ItemFestivalAffinity affinity, FestivalCalendar calendar) {
        return new FestivalAffinityInput(
                affinity.getFestivalCode(), calendar.getFestivalDate(), affinity.getAffinity());
    }
}
