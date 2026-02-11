package com.ssafy.meari.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    // Exchange & Queue 상수
    public static final String ANALYSIS_EXCHANGE = "analysis.exchange";
    public static final String REQUEST_QUEUE = "analysis.requests";
    public static final String RESULT_QUEUE = "analysis.results";
    public static final String REQUEST_ROUTING_KEY = "analysis.request";
    public static final String RESULT_ROUTING_KEY = "analysis.result";

    // TTL 설정 (10분)
    private static final int MESSAGE_TTL = 600000;

    /**
     * Analysis Exchange 생성 (Direct)
     */
    @Bean
    public DirectExchange analysisExchange() {
        DirectExchange exchange = new DirectExchange(ANALYSIS_EXCHANGE, true, false);
        log.info("RabbitMQ DirectExchange 생성: {}", ANALYSIS_EXCHANGE);
        return exchange;
    }

    /**
     * Analysis Request Queue 생성 (Spring → FastAPI)
     */
    @Bean
    public Queue requestQueue() {
        Queue queue = new Queue(REQUEST_QUEUE, true, false, false);
        log.info("RabbitMQ Queue 생성: {}", REQUEST_QUEUE);
        return queue;
    }

    /**
     * Analysis Result Queue 생성 (FastAPI → Spring)
     */
    @Bean
    public Queue resultQueue() {
        Queue queue = new Queue(RESULT_QUEUE, true, false, false);
        log.info("RabbitMQ Queue 생성: {}", RESULT_QUEUE);
        return queue;
    }

    /**
     * Request Queue Binding
     */
    @Bean
    public Binding requestBinding(Queue requestQueue, DirectExchange analysisExchange) {
        Binding binding = BindingBuilder.bind(requestQueue)
                .to(analysisExchange)
                .with(REQUEST_ROUTING_KEY);
        log.info("RabbitMQ Binding 생성: {} -> {} (key: {})",
                ANALYSIS_EXCHANGE, REQUEST_QUEUE, REQUEST_ROUTING_KEY);
        return binding;
    }

    /**
     * Result Queue Binding
     */
    @Bean
    public Binding resultBinding(Queue resultQueue, DirectExchange analysisExchange) {
        Binding binding = BindingBuilder.bind(resultQueue)
                .to(analysisExchange)
                .with(RESULT_ROUTING_KEY);
        log.info("RabbitMQ Binding 생성: {} -> {} (key: {})",
                ANALYSIS_EXCHANGE, RESULT_QUEUE, RESULT_ROUTING_KEY);
        return binding;
    }

    /**
     * JSON 메시지 변환기
     */
    @Bean
    public MessageConverter messageConverter() {
        log.info("RabbitMQ Jackson2JsonMessageConverter 등록");
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 설정
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        log.info("RabbitMQ Template 생성 완료");
        return rabbitTemplate;
    }
}
