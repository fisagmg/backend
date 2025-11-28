package com.labhub.CveLabhubBack.cve.repository;

import com.labhub.CveLabhubBack.cve.entity.Cve;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * CVE 엔티티에 대한 동적 필터링을 위한 Specification 클래스
 */
public class CveSpecification {

    /**
     * 동적 필터링을 위한 Specification 생성
     *
     * @param relatedDomain 관련 도메인 필터 (optional)
     * @param year 연도 필터 (optional)
     * @param labOs Lab OS 필터 (optional)
     * @return Specification<Cve>
     */
    public static Specification<Cve> withFilters(String relatedDomain, Integer year, String labOs) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // related_domain 필터
            if (relatedDomain != null && !relatedDomain.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.upper(root.get("relatedDomain")),
                    relatedDomain.trim().toUpperCase()
                ));
            }

            // year 필터
            if (year != null) {
                predicates.add(criteriaBuilder.equal(root.get("year"), year));
            }

            // lab_os 필터 (대소문자 무시)
            if (labOs != null && !labOs.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.upper(root.get("labOs")),
                    "%" + labOs.trim().toUpperCase() + "%"
                ));
            }

            // CVSS Score 내림차순 정렬 적용
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("cvssScore")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}


