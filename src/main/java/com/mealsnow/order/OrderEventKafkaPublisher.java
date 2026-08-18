package com.mealsnow.order;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventKafkaPublisher {

    public static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderStatusChanged> kafkaTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public OrderEventKafkaPublisher(KafkaTemplate<String, OrderStatusChanged> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChanged event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    }
}