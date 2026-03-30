package com.ecommerce.model;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    CREATED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(PENDING_PAYMENT, CANCELLED, FAILED);
        }
    },
    PENDING_PAYMENT {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(PAID, FAILED, CANCELLED);
        }
    },
    PAID {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },
    FAILED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    },
    PARTIALLY_RETURNED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return EnumSet.noneOf(OrderStatus.class);
        }
    };

    public abstract Set<OrderStatus> allowedTransitions();

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }
}
