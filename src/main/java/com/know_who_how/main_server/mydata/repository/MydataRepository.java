package com.know_who_how.main_server.mydata.repository;

import com.know_who_how.main_server.global.entity.Mydata.Mydata;
import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MyData Entity DB 접근(CRUD)
 */
public interface MydataRepository extends JpaRepository<Mydata, Long> {

    /**
     * 주어진 사용자에 매핑된 Mydata 엔티티를 조회
     * 존재하지 않으면 Optional.empty() 반환
     */
    Optional<Mydata> findByUser(User user);

    /**
     * 사용자의 마이데이터 연동 여부를 빠르게 확인
     * (엔티티 로딩 없이 존재 여부만 체크)
     */
    boolean existsByUser(User user);

    /**
     * 주어진 사용자에 매핑된 Mydata 엔티티를 삭제
     */
    void deleteByUser(User user);
}
