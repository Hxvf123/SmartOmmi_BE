package com.smartomni.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * UC-32/UC-33: Khai bao Exchange/Queue cho luong dong bo don hang qua Webhook.
 * Durable Queue dam bao khong mat message khi service restart (FR-062).
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "smartomni.order.exchange";
    public static final String ORDER_QUEUE = "smartomni.order.incoming.queue";
    public static final String ORDER_ROUTING_KEY = "order.incoming";

    // Dead-letter queue cho message that bai qua nhieu lan (bo sung do tin cay)
    public static final String ORDER_DLQ = "smartomni.order.incoming.dlq";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
                .build();
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
