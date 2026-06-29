package com.airfreight.repository;

import com.airfreight.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByType(String type);
    List<Customer> findByCountryContaining(String country);
}