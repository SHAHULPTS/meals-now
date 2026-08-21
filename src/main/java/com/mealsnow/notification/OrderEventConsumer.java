package com.mealsnow.notification;

import com.mealsnow.order.OrderEventKafkaPublisher;
import com.mealsnow.order.OrderStatusChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private final SimpMessagingTemplate messagingTemplate;

    public OrderEventConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = OrderEventKafkaPublisher.TOPIC)
    public void onOrderStatusChanged(OrderStatusChanged event) {
        log.info("NOTIFY customer {} — order {} moved {} -> {}",
                event.customerId(), event.orderId(), event.oldStatus(), event.newStatus());

        messagingTemplate.convertAndSendToUser(
                event.customerId().toString(),   // MUST equal the Principal name set at CONNECT
                "/queue/orders",                 // browser subscribes to /user/queue/orders
                event);                          // serialized to JSON by the default converter
    }
}