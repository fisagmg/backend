package com.labhub.CveLabhubBack.cve_lab.entity;

/**
 * Lab 세션 상태
 * - ACTIVE: VM 생성 완료 ~ 종료 전까지
 * - TERMINATED: VM 종료 후
 */
public enum LabStatus {
    ACTIVE,      // VM 생성 완료, 실습 중
    TERMINATED   // VM 종료됨
}

