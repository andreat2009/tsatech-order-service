package com.newproject.order.repository;

import com.newproject.order.domain.OrderCustomFieldValue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderCustomFieldValueRepository extends JpaRepository<OrderCustomFieldValue, Long> {
    List<OrderCustomFieldValue> findByOrderIdOrderByIdAsc(Long orderId);

    void deleteByOrderId(Long orderId);
}
