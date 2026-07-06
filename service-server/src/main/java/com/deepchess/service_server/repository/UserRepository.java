package com.deepchess.service_server.repository;

import com.deepchess.service_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 구글 고유 식별자로 기존 회원인지 확인하기 위함
    Optional<User> findByGoogleUid(String googleUid);
}