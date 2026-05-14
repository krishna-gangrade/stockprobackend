package com.stockpro.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderResponse {
    private PaymentResponse payment;
    private String razorpayKeyId;
    private String razorpayOrderId;
    private Integer amount;
    private String currency;
}
