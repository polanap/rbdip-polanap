package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Модуль расчёта цены заказа: скидка за объём позиции, скидка по типу
 * клиента, купон и скидка за крупный заказ.
 */
@Component
public class PricingCalculator {

    private static final String CUSTOMER_VIP = "vip";
    private static final String CUSTOMER_WHOLESALE = "wholesale";
    private static final String COUPON_FIXED_AMOUNT = "SAVE10";
    private static final String COUPON_PERCENT = "SAVE20PERCENT";

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_MULTIPLIER = BigDecimal.valueOf(0.95);
    private static final BigDecimal VIP_DISCOUNT_MULTIPLIER = BigDecimal.valueOf(0.9);
    private static final BigDecimal WHOLESALE_DISCOUNT_MULTIPLIER = BigDecimal.valueOf(0.85);
    private static final BigDecimal COUPON_FIXED_DISCOUNT = BigDecimal.TEN;
    private static final BigDecimal COUPON_PERCENT_MULTIPLIER = BigDecimal.valueOf(0.8);
    private static final BigDecimal LARGE_ORDER_THRESHOLD = BigDecimal.valueOf(1000);
    private static final BigDecimal LARGE_ORDER_MULTIPLIER = BigDecimal.valueOf(0.98);
    private static final int MONEY_SCALE = 2;

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;
        for (LineItem item : items) {
            total = total.add(linePrice(item));
        }
        total = applyCustomerDiscount(total, customerType);
        total = applyCoupon(total, couponCode);
        total = total.max(BigDecimal.ZERO);
        total = applyLargeOrderDiscount(total);
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal linePrice(LineItem item) {
        BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
        if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
            return linePrice.multiply(BULK_DISCOUNT_MULTIPLIER);
        }
        return linePrice;
    }

    private BigDecimal applyCustomerDiscount(BigDecimal total, String customerType) {
        if (CUSTOMER_VIP.equals(customerType)) {
            return total.multiply(VIP_DISCOUNT_MULTIPLIER);
        }
        if (CUSTOMER_WHOLESALE.equals(customerType)) {
            return total.multiply(WHOLESALE_DISCOUNT_MULTIPLIER);
        }
        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (COUPON_FIXED_AMOUNT.equals(couponCode)) {
            return total.subtract(COUPON_FIXED_DISCOUNT);
        }
        if (COUPON_PERCENT.equals(couponCode)) {
            return total.multiply(COUPON_PERCENT_MULTIPLIER);
        }
        return total;
    }

    private BigDecimal applyLargeOrderDiscount(BigDecimal total) {
        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            return total.multiply(LARGE_ORDER_MULTIPLIER);
        }
        return total;
    }
}
