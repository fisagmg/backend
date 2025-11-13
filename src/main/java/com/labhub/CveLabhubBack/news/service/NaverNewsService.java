package com.labhub.CveLabhubBack.news.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labhub.CveLabhubBack.cve.entity.Cve;
import com.labhub.CveLabhubBack.cve.repository.CveRepository;
import com.labhub.CveLabhubBack.news.dto.NewsResponse;
import com.labhub.CveLabhubBack.news.entity.CveNewsMapping;
import com.labhub.CveLabhubBack.news.entity.News;
import com.labhub.CveLabhubBack.news.repository.CveNewsMappingRepository;
import com.labhub.CveLabhubBack.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NaverNewsService {

    private final NewsRepository newsRepository;
    private final CveRepository cveRepository;
    private final CveNewsMappingRepository cveNewsMappingRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${naver.api.url}")
    private String naverApiUrl;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private static final String DEFAULT_THUMBNAIL = "https://cvelabhub.io/default-thumb.png";
    private static final Pattern CVE_PATTERN = Pattern.compile("CVE-(\\d{4})-(\\d+)", Pattern.CASE_INSENSITIVE);

    @Transactional
    public void crawlAndSaveNews() {
        log.info("🕒 [NaverNewsService] 뉴스 수집 시작");

        int totalSaved = 0;
        int totalMapped = 0;

        String[] keywords = {"CVE-2025", "CVE-2024", "CVE-2023"};

        for (String keyword : keywords) {
            try {
                List<News> newsItems = fetchNewsFromNaver(keyword);

                for (News news : newsItems) {
                    if (newsRepository.existsByExternalUrl(news.getExternalUrl())) {
                        log.debug("중복 뉴스 건너뜀: {}", news.getTitle());
                        continue;
                    }

                    News savedNews = newsRepository.save(news);
                    totalSaved++;
                    log.info("✅ 뉴스 저장: [{}] {}", savedNews.getPublisher(), savedNews.getTitle());

                    int mappedCount = mapCveToNews(savedNews);
                    totalMapped += mappedCount;
                }

            } catch (Exception e) {
                log.error("❌ 키워드 [{}] 뉴스 수집 실패: {}", keyword, e.getMessage(), e);
            }
        }

        log.info("🕒 [NaverNewsService] 뉴스 수집 완료 — 신규 저장 {}건, CVE 자동 매핑 {}건", totalSaved, totalMapped);
    }

    private List<News> fetchNewsFromNaver(String keyword) {
        List<News> newsList = new ArrayList<>();

        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = naverApiUrl + "?query=" + encodedKeyword + "&display=10&sort=date";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("items");

                for (JsonNode item : items) {
                    String title = cleanHtml(item.path("title").asText());
                    String description = cleanHtml(item.path("description").asText());
                    String link = item.path("link").asText();
                    String originLink = item.path("originallink").asText();
                    String targetUrl = originLink != null && !originLink.isBlank() ? originLink : link;

                    ArticleMetadata metadata = resolveArticleMetadata(targetUrl);

                    News news = News.builder()
                            .publisher(metadata.publisher())
                            .title(title)
                            .firstLine(description)
                            .thumbnail(metadata.thumbnail())
                            .externalUrl(targetUrl)
                            .build();

                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            log.error("네이버 API 호출 실패: keyword={}", keyword, e);
        }

        return newsList;
    }

    private String cleanHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    private ArticleMetadata resolveArticleMetadata(String url) {
        if (url == null || url.isBlank()) {
            return new ArticleMetadata(DEFAULT_THUMBNAIL, "네이버뉴스");
        }

        try {
            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();

            String thumbnail = firstNonEmpty(
                    selectMetaContent(document, "meta[property=og:image]"),
                    selectMetaContent(document, "meta[name=twitter:image]"),
                    DEFAULT_THUMBNAIL
            );

            String publisher = firstNonEmpty(
                    selectMetaContent(document, "meta[property=og:site_name]"),
                    selectMetaContent(document, "meta[name=twitter:site]"),
                    extractPublisherFromUrl(url),
                    "네이버뉴스"
            );

            return new ArticleMetadata(thumbnail, sanitizePublisher(publisher));

        } catch (Exception e) {
            log.debug("기사 메타데이터 추출 실패: {}", url, e);
            return new ArticleMetadata(DEFAULT_THUMBNAIL, extractPublisherFromUrl(url));
        }
    }

    private String selectMetaContent(Document document, String selector) {
        Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String content = element.attr("content");
        return content != null && !content.isBlank() ? content.trim() : null;
    }

    private String extractPublisherFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return "네이버뉴스";
            }
            if (host.contains("naver.com")) {
                return "네이버뉴스";
            }
            host = host.replaceFirst("^www\\.", "");
            int dotIndex = host.indexOf('.');
            if (dotIndex > 0) {
                return host.substring(0, dotIndex).toUpperCase();
            }
            return host.toUpperCase();
        } catch (Exception e) {
            return "네이버뉴스";
        }
    }

    private String sanitizePublisher(String publisher) {
        if (publisher == null) {
            return "네이버뉴스";
        }
        String value = publisher.trim();
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        return value.isBlank() ? "네이버뉴스" : value;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record ArticleMetadata(String thumbnail, String publisher) {}

    @Transactional
    public int mapCveToNews(News news) {
        int mappedCount = 0;
        String combinedText = news.getTitle() + " " + news.getFirstLine();

        Matcher matcher = CVE_PATTERN.matcher(combinedText);

        while (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int num = Integer.parseInt(matcher.group(2));

                Optional<Cve> cveOpt = cveRepository.findByYearAndNum(year, num);

                if (cveOpt.isPresent()) {
                    Cve cve = cveOpt.get();

                    if (!cveNewsMappingRepository.existsByNewsIdAndCveId(news.getId(), cve.getId())) {
                        CveNewsMapping mapping = CveNewsMapping.builder()
                                .newsId(news.getId())
                                .cveId(cve.getId())
                                .build();

                        cveNewsMappingRepository.save(mapping);
                        mappedCount++;
                        log.info("🔗 CVE 매핑 성공: CVE-{}-{} ↔ News ID {}", year, num, news.getId());
                    }
                }

            } catch (Exception e) {
                log.error("CVE 매핑 중 오류: {}", e.getMessage());
            }
        }

        return mappedCount;
    }

    public List<NewsResponse> getTop4News() {
        return newsRepository.findTop4ByOrderByCreatedAtDesc()
                .stream()
                .map(NewsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<NewsResponse> getAllNews() {
        return newsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NewsResponse::fromEntity)
                .collect(Collectors.toList());
    }
}


