package com.labhub.CveLabhubBack.cve.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cve",
        indexes = {
                @Index(name = "idx_cve_year_num", columnList = "year, num")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cve_name", columnNames = {"name"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // 예: CVE-2025-1302

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer num;

    @Column(columnDefinition = "TEXT")
    private String outline;

    @Column(name = "related_domain", length = 100)
    private String relatedDomain;

    @Column(name = "cvss_score")
    private Double cvssScore;
}

