-- Medium 난이도 CVE 2개 추가 (CVSS 4.0~6.9)
INSERT INTO cve (cvss_score, name, year, num, related_domain, lab_os, outline) VALUES
(5.3, 'CVE-2024-33891', 2024, 33891, 'WEB', 'Ubuntu', 'WordPress Plugin XSS 취약점'),
(6.1, 'CVE-2023-28475', 2023, 28475, 'Application', 'Linux', 'Node.js 패키지 정보 노출 취약점');
