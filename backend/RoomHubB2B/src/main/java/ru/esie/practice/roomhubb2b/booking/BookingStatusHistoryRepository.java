package ru.esie.practice.roomhubb2b.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingStatusHistoryRepository
        extends JpaRepository<BookingStatusHistoryEntity, Long> {

    List<BookingStatusHistoryEntity> findByBookingIdOrderByCreatedAtAscIdAsc(Long bookingId);

    long countByBookingId(Long bookingId);

    void deleteByBookingId(Long bookingId);
}
