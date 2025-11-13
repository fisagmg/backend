package com.labhub.CveLabhubBack.cve.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "cve")
public class Cve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer num;

    @Column(nullable = false, length = 100)
    private String outline;

    @Column(name = "lab_os", nullable = false, length = 100)
    private String labOs;

    @Column(name = "related_domain", nullable = false, length = 20)
    private String relatedDomain;

    @Column(name = "cvss_score", nullable = false)
    private Float cvssScore;
}