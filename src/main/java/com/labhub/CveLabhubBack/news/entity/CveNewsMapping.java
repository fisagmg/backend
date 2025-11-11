package com.labhub.CveLabhubBack.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "cve_news_mapping_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CveNewsMapping.CveNewsMappingId.class)
public class CveNewsMapping {

    @Id
    @Column(name = "news_id")
    private Long newsId;

    @Id
    @Column(name = "cve_id")
    private Long cveId;

    // Composite Key
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CveNewsMappingId implements Serializable {
        private Long newsId;
        private Long cveId;
    }
}

