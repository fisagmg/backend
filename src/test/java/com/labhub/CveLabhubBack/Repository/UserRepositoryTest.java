package com.labhub.CveLabhubBack.Repository;

import com.labhub.CveLabhubBack.auth.Repository.UserRepository;
import com.labhub.CveLabhubBack.auth.entity.UserEntity;
import com.labhub.CveLabhubBack.auth.entity.UserRole;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest // ✅ JPA 전용 테스트 (DB 연결 포함)
@Transactional
@Rollback(false) // ⚠️ false로 하면 실제 DB에 반영됨 (테스트용)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testUserSaveAndFind() {
        // 1️⃣ 새 사용자 생성
        UserEntity user = new UserEntity();
        user.setKcUserId("test-uuid-123");
        user.setEmail("tester@company.com");
        user.setFirstName("Min");
        user.setLastName("Kyung");
        user.setPhone("010-1234-5678");
        user.setRole(UserRole.USER);

        // 2️⃣ DB에 저장
        userRepository.save(user);

        // 3️⃣ 이메일로 검색
        UserEntity found = userRepository.findByEmail("tester@company.com").orElseThrow();

        // 4️⃣ 검증
        assertThat(found.getEmail()).isEqualTo("tester@company.com");
        assertThat(found.getKcUserId()).isEqualTo("test-uuid-123");

        System.out.println("✅ 저장된 사용자: " + found.getFirstName() + " " + found.getLastName());
    }
}
