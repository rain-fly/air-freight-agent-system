package com.airfreight.repository;

import com.airfreight.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingNo(String bookingNo);
    Optional<Booking> findByShipmentId(Long shipmentId);
}