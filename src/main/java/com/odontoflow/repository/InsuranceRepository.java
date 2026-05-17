package com.odontoflow.repository;

import com.odontoflow.entity.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InsuranceRepository extends JpaRepository<Insurance, UUID> {
    List<Insurance> findAllByOrderByNameAsc();
}
