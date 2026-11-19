package com.rbdip.bookstore.order;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Уведомление клиента о созданном заказе. Реальный почтовый транспорт в
 * учебном проекте не настроен - отправка письма эмулируется записью в лог.
 */
@Component
public class OrderNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(OrderNotifier.class);

    public void sendConfirmation(String customerName, Long orderId, BigDecimal total) {
        LOG.info("[email] Dear {}, your order #{} for {} has been placed.", customerName, orderId, total);
    }
}
