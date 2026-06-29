package com.airfreight.repository;

import com.airfreight.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByShipmentNo(String shipmentNo);
    List<Shipment> findByStatus(String status);
    List<Shipment> findByShipperNameContaining(String shipperName);
    List<Shipment> findByConsigneeNameContaining(String consigneeName);
    List<Shipment> findByOriginAirportAndDestAirport(String origin, String dest);
}