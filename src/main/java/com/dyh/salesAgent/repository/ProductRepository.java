package com.dyh.salesAgent.repository;
import com.dyh.salesAgent.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuCode(String skuCode); // 根据SKU编码查询

    List<Product> findByCategory(String category);

    List<Product> findByStatus(String status);
}
