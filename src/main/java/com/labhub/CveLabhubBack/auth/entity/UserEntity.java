package com.labhub.CveLabhubBack.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity // DB 테이블과 매핑됨
@Table(name = "users",
        indexes = {
                @Index(name="idx_users_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name="uk_users_kc_user_id", columnNames = {"kc_user_id"}),
                @UniqueConstraint(name="uk_users_email", columnNames = {"email"})
        })
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kc_user_id", nullable = false, length = 64)
    private String kcUserId;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING) // Enum 이름 그대로 문자열로 저장 (USER / ADMIN)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
