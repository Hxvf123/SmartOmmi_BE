package com.smartomni.catalog.repository;

import com.smartomni.catalog.entity.PromotionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

@org.springframework.transaction.annotation.Transactional(readOnly = true)
public interface PromotionEventRepository extends JpaRepository<PromotionEvent, Long> {
    List<PromotionEvent> findByTenantIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long tenantId, LocalDate date1, LocalDate date2);
}
