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
    public ReportResponse createReport(ReportCreateRequest request, Long userId) {
        //log.info("Creating new report for userId={}, cveId={}", userId, request.getCveId());
        
        // 디버깅: Service에서 받은 request 값 확인
        //log.debug("🔍 [SERVICE] Received request - userId={}, cveId={}, name={}", 
                userId, request.getCveId(), request.getName());

        // 같은 userId + cveId 조합의 보고서가 이미 있는지 확인
        List<Report> existingReports = reportRepository.findByCveIdAndUserId(request.getCveId(), userId);
        
        //log.debug("🔍 [SERVICE] Checking existing reports - found {} reports for userId={}, cveId={}", 
                existingReports.size(), userId, request.getCveId());
        
        if (!existingReports.isEmpty()) {
            // 이미 존재하면 기존 보고서 반환
            Report existingReport = existingReports.get(0);
            String presignedUrl = s3StorageUtil.generatePresignedUrl(existingReport.getFileUrl());
            //log.info("Report already exists for userId={}, cveId={}, returning existing report id={}", 
                    userId, request.getCveId(), existingReport.getId());
            //log.debug("🔍 [SERVICE] Returning existing report - reportId={}, userId={}, cveId={}", 
                    existingReport.getId(), existingReport.getUserId(), existingReport.getCveId());
            return ReportResponse.fromEntityWithPresignedUrl(existingReport, presignedUrl);
        }

        // S3에서 템플릿 복제
        //log.debug("🔍 [SERVICE] Copying template to S3 - userId={}, cveId={}", 
                userId, request.getCveId());
        String s3Key = s3StorageUtil.copyTemplateToNewReport(userId, request.getCveId());

        // DB에 저장
        Report report = Report.builder()
                .userId(userId)
                .cveId(request.getCveId())
                .name(request.getName())
                .fileUrl(s3Key)
                .status("active")
                .version(1)
                .build();

        //log.debug("🔍 [SERVICE] Saving report to DB - userId={}, cveId={}, name={}, fileUrl={}", 
                report.getUserId(), report.getCveId(), report.getName(), report.getFileUrl());
        
        Report savedReport = reportRepository.save(report);
        
        //log.debug("🔍 [SERVICE] Report saved - reportId={}, userId={}, cveId={}", 
                savedReport.getId(), savedReport.getUserId(), savedReport.getCveId());

        // Presigned URL 생성
        String presignedUrl = s3StorageUtil.generatePresignedUrl(s3Key);

        //log.info("Report created successfully with id={}, userId={}", savedReport.getId(), savedReport.getUserId());
        return ReportResponse.fromEntityWithPresignedUrl(savedReport, presignedUrl);
    }

    /**
     * 2️⃣ 보고서 업로드 (저장) - 덮어쓰기 방식
     * PUT /api/reports/{id}/file
     * 동일 S3 경로에 덮어쓰기, updated_at만 갱신
     */
    @Transactional
    public ReportUploadResponse uploadReportFile(Long reportId, Long userId, MultipartFile file) {
        //log.info("Uploading file for reportId={}, userId={} (overwrite mode)", reportId, userId);

        // 보고서 조회
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx files are allowed");
        }

        // 기존 S3 키 사용 (덮어쓰기)
        String s3Key = report.getFileUrl();
        
        // S3에 업로드 (동일 경로에 덮어쓰기)
        s3StorageUtil.uploadFile(file, s3Key);

        // DB updated_at 갱신 (JPA @PreUpdate로 자동 처리)
        Report updatedReport = reportRepository.save(report);

        //log.info("Report file uploaded successfully (overwrite). S3 Key: {}", s3Key);

        return ReportUploadResponse.builder()
                .reportId(updatedReport.getId())
                .fileUrl(s3Key)
                .version(updatedReport.getVersion())
                .message("파일이 성공적으로 업로드되었습니다. (덮어쓰기)")
                .build();
    }

    /**
     * 3️⃣ 보고서 목록 조회
     * GET /api/reports/me
     */
    public List<ReportResponse> getMyReports(Long userId) {
        //log.info("Fetching reports for userId={}", userId);

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
        //log.info("Generating download URL for reportId={}, userId={}", reportId, userId);

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
        //log.info("Deleting report reportId={}, userId={}", reportId, userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        // S3 파일 삭제
        try {
            s3StorageUtil.deleteFile(report.getFileUrl());
            //log.info("S3 file deleted successfully: {}", report.getFileUrl());
        } catch (Exception e) {
            log.error("Failed to delete S3 file: {}", report.getFileUrl(), e);
            // S3 삭제 실패해도 DB는 삭제 진행 (선택사항)
        }

        // 수정 후
        // DB에서 Hard delete (실제 row 삭제)
        reportRepository.delete(report);
        reportRepository.flush();

        //log.info("Report deleted successfully (DB soft delete + S3 hard delete). reportId={}", reportId);
    }

    /**
     * 보고서 상세 조회
     */
    public ReportResponse getReportById(Long reportId, Long userId) {
        //log.info("Fetching report detail for reportId={}, userId={}", reportId, userId);

        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        return ReportResponse.fromEntity(report);
    }

    /**
     * CVE ID로 보고서 조회
     */
    public List<ReportResponse> getReportsByCveId(String cveId, Long userId) {
        //log.info("Fetching reports for cveId={}, userId={}", cveId, userId);

        List<Report> reports = reportRepository.findByCveIdAndUserId(cveId, userId);

        return reports.stream()
                .map(ReportResponse::fromEntity)
                .collect(Collectors.toList());
    }
}

