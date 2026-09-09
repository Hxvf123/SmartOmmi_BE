package com.smartomni.integration.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai bao lai cung 1 Exchange voi smartomni-service-order (idempotent - RabbitMQ
 * cho phep nhieu service cung khai bao 1 exchange neu cau hinh giong het nhau).
 * Integration Service la ben PRODUCER (day message khi nhan Webhook - UC-32),
 * Order Service la ben CONSUMER (xu ly bat dong bo - UC-33).
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "smartomni.order.exchange";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
