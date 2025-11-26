package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Asset.FinancialProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialProductRepository extends JpaRepository<FinancialProduct, Long> {
    Optional<FinancialProduct> findByProductName(String productName);
    List<FinancialProduct> findByProductType(FinancialProduct.ProductType productType);
}
