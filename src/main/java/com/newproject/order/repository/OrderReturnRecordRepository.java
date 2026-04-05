package com.newproject.order.repository;

import com.newproject.order.domain.OrderReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReturnRecordRepository extends JpaRepository<OrderReturnRecord, Long> {
    void deleteByOrderId(Long orderId);
}
