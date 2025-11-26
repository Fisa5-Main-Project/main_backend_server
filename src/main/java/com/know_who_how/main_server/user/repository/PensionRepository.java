package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Asset.Pension.Pension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PensionRepository extends JpaRepository<Pension, Long> {
}
