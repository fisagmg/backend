package com.labhub.CveLabhubBack.report.service;

import com.labhub.CveLabhubBack.report.dto.*;
import com.labhub.CveLabhubBack.report.entity.Report;
import com.labhub.CveLabhubBack.report.exception.ReportNotFoundException;
import com.labhub.CveLabhubBack.report.repository.ReportRepository;
import com.labhub.CveLabhubBack.report.util.S3StorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Profile("!test")
public class ReportService {

    private final ReportRepository reportRepository;
    private final S3StorageUtil s3StorageUtil;

    /**
     * 1️⃣ 보고서 생성 (템플릿 복제)
     * POST /api/reports
     */
    @Transactional
    public ReportResponse createReport(ReportCreateRequest request) {
        log.info("Creating new report for userId={}, cveId={}", request.getUserId(), request.getCveId());

        // 같은 userId + cveId 조합의 보고서가 이미 있는지 확인
        List<Report> existingReports = reportRepository.findByCveIdAndUserId(request.getCveId(), request.getUserId());
        
        if (!existingReports.isEmpty()) {
            // 이미 존재하면 기존 보고서 반환
            Report existingReport = existingReports.get(0);
            String presignedUrl = s3StorageUtil.generatePresignedUrl(existingReport.getFileUrl());
            log.info("Report already exists for userId={}, cveId={}, returning existing report id={}", 
                    request.getUserId(), request.getCveId(), existingReport.getId());
            return ReportResponse.fromEntityWithPresignedUrl(existingReport, presignedUrl);
        }

        // S3에서 템플릿 복제
        String s3Key = s3StorageUtil.copyTemplateToNewReport(request.getUserId(), request.getCveId());

        // DB에 저장
        Report report = Report.builder()
                .userId(request.getUserId())
                .cveId(request.getCveId())
                .name(request.getName())
                .fileUrl(s3Key)
                .status("active")
                .version(1)
                .build();

        Report savedReport = reportRepository.save(report);

        // Presigned URL 생성
        String presignedUrl = s3StorageUtil.generatePresignedUrl(s3Key);

        log.info("Report created successfully with id={}", savedReport.getId());
        return ReportResponse.fromEntityWithPresignedUrl(savedReport, presignedUrl);
    }

    /**
     * 2️⃣ 보고서 업로드 (저장)
     * PUT /api/reports/{id}/file
     */
    @Transactional
    public ReportUploadResponse uploadReportFile(Long reportId, Long userId, MultipartFile file) {
        log.info("Uploading file for reportId={}, userId={}", reportId, userId);

        // 보고서 조회
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx files are allowed");
        }

        // 새 버전으로 키 생성
        report.incrementVersion();
        String newS3Key = s3StorageUtil.generateNewVersionKey(report.getFileUrl(), report.getVersion());

        // S3에 업로드
        s3StorageUtil.uploadFile(file, newS3Key);

        // DB 업데이트
        report.setFileUrl(newS3Key);
        Report updatedReport = reportRepository.save(report);

        log.info("Report file uploaded successfully. New version={}", updatedReport.getVersion());

        return ReportUploadResponse.builder()
                .reportId(updatedReport.getId())
                .fileUrl(newS3Key)
                .version(updatedReport.getVersion())
                .message("파일이 성공적으로 업로드되었습니다.")
                .build();
    }

    /**
     * 3️⃣ 보고서 목록 조회
     * GET /api/reports/me
     */
    public List<ReportResponse> getMyReports(Long userId) {
        log.info("Fetching reports for userId={}", userId);

        List<Report> reports = reportRepository.findActiveReportsByUserId(userId);

        return reports.stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 4️⃣ 보고서 다운로드
     * GET /api/reports/{id}/download
     */
    public PresignedUrlResponse downloadReport(Long reportId, Long userId) {
        log.info("Generating download URL for reportId={}, userId={}", reportId, userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        String presignedUrl = s3StorageUtil.generatePresignedUrl(report.getFileUrl());

        return PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .expiresInMinutes(5L)
                .message("다운로드 링크가 생성되었습니다. 5분 이내에 사용하세요.")
                .build();
    }

    /**
     * 5️⃣ 보고서 삭제 (Hard Delete - DB + S3)
     * DELETE /api/reports/{id}
     */
    @Transactional
    public void deleteReport(Long reportId, Long userId) {
        log.info("Deleting report reportId={}, userId={}", reportId, userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        // S3 파일 삭제
        try {
            s3StorageUtil.deleteFile(report.getFileUrl());
            log.info("S3 file deleted successfully: {}", report.getFileUrl());
        } catch (Exception e) {
            log.error("Failed to delete S3 file: {}", report.getFileUrl(), e);
            // S3 삭제 실패해도 DB는 삭제 진행 (선택사항)
        }

        // DB에서 Soft delete
        report.softDelete();
        reportRepository.save(report);

        log.info("Report deleted successfully (DB soft delete + S3 hard delete). reportId={}", reportId);
    }

    /**
     * 보고서 상세 조회
     */
    public ReportResponse getReportById(Long reportId, Long userId) {
        log.info("Fetching report detail for reportId={}, userId={}", reportId, userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        return ReportResponse.fromEntity(report);
    }

    /**
     * CVE ID로 보고서 조회
     */
    public List<ReportResponse> getReportsByCveId(String cveId, Long userId) {
        log.info("Fetching reports for cveId={}, userId={}", cveId, userId);

        List<Report> reports = reportRepository.findByCveIdAndUserId(cveId, userId);

        return reports.stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

