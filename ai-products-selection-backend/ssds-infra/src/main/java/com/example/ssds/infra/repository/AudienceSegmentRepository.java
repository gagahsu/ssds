package com.example.ssds.infra.repository;

import com.example.ssds.infra.entity.AudienceSegment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 去識別化客群統計（規格書 §7.2 audience_segment、§5.2.4）。 */
@Repository
public interface AudienceSegmentRepository extends JpaRepository<AudienceSegment, Long> {

    Optional<AudienceSegment> findByAudienceCode(String audienceCode);
}
