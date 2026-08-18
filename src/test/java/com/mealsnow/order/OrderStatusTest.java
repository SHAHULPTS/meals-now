package com.mealsnow.order;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


public class OrderStatusTest {

    @Test
    void legalTransitionsAreAllowed() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.ACCEPTED)).isTrue();
        assertThat(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.PREPARING)).isTrue();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isTrue();
        assertThat(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        // TODO add the cancel/reject edges: PLACED->CANCELLED, PLACED->REJECTED,
        //      ACCEPTED->CANCELLED, PREPARING->CANCELLED

        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.REJECTED)).isTrue();
        assertThat(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();

    }

    @Test
    void illegalJumpsAreRejected() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.PREPARING)).isFalse();
        assertThat(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.READY)).isFalse();
        // TODO: READY->CANCELLED should be false (too late to cancel)
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void terminalStatesHaveNoExits() {
        // A property test: from each terminal state, NO target is reachable.
        for (OrderStatus terminal : new OrderStatus[]{
                OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REJECTED}) {
            for (OrderStatus target : OrderStatus.values()) {
                assertThat(terminal.canTransitionTo(target))
                        .as("%s -> %s must be illegal", terminal, target)
                        .isFalse();
            }
        }

    }
}
