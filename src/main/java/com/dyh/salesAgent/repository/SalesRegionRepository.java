package com.dyh.salesAgent.repository;
import com.dyh.salesAgent.entity.SalesRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesRegionRepository extends JpaRepository<SalesRegion, Long> {

    Optional<SalesRegion> findByName(String name);
}
