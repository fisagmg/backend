package com.labhub.CveLabhubBack;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

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
}
