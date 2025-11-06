package com.labhub.CveLabhubBack;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DbConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        // 단순히 DB 연결 후 쿼리 수행 테스트
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        System.out.println("DB 연결 테스트 결과: " + result);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void testUserTableList() {
        // users 테이블 존재 여부 확인 (존재하지 않으면 예외 발생)
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");

            System.out.println("✅ 현재 DB 유저 목록 (" + users.size() + "명):");
            for (Map<String, Object> row : users) {
                System.out.println(" - " + row);
            }

            // 단순 검증: 테이블이 존재하면 result != null
            assertThat(users).isNotNull();

        } catch (Exception e) {
            System.err.println("⚠️ users 테이블이 존재하지 않거나 쿼리 실패: " + e.getMessage());
            throw e;
        }
    }

    @Test
    void showTableNames() {
        List<Map<String, Object>> tables =
                jdbcTemplate.queryForList("SHOW TABLES");

        System.out.println("✅ 현재 DB에 존재하는 테이블 목록:");
        for (Map<String, Object> row : tables) {
            // MySQL의 SHOW TABLES 결과는 컬럼 이름이 “Tables_in_<DB명>”
            String tableName = row.values().iterator().next().toString();
            System.out.println(" - " + tableName);
        }
    }
}
