package com.koosco.inventoryservice.infra.config

import com.koosco.common.core.event.CloudEvent
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener
import org.springframework.kafka.support.serializer.JsonDeserializer

@EnableKafka
@Configuration
class KafkaConfig(private val kafkaProperties: KafkaProperties) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun consumerFactory(): ConsumerFactory<String, CloudEvent<*>> {
        val props = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaProperties.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "inventory-service",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG to
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor",
        )

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            JsonDeserializer(CloudEvent::class.java).apply {
                addTrustedPackages("*")
            },
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, CloudEvent<*>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, CloudEvent<*>>()
        factory.consumerFactory = consumerFactory()
        factory.containerProperties.ackMode =
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.containerProperties.setConsumerRebalanceListener(inventoryRebalanceListener())
        return factory
    }

    @Bean
    fun inventoryRebalanceListener(): ConsumerAwareRebalanceListener = object : ConsumerAwareRebalanceListener {

        override fun onPartitionsRevokedBeforeCommit(
            consumer: Consumer<*, *>,
            partitions: Collection<TopicPartition>,
        ) {
            log.warn("⚠️ Rebalance 시작 - 파티션 revoke 전: {}", partitions)

            // 현재까지 처리된 offset 커밋
            try {
                consumer.commitSync()
                log.info("✅ Offset 커밋 완료 before revoke")
            } catch (e: Exception) {
                log.error("❌ Offset 커밋 실패: {}", e.message, e)
            }
        }

        override fun onPartitionsRevoked(partitions: Collection<TopicPartition>) {
            log.info("📉 파티션 revoke 완료: {}", partitions)
        }

        override fun onPartitionsAssigned(consumer: Consumer<*, *>, partitions: Collection<TopicPartition>) {
            log.info("📈 새 파티션 할당됨: {}", partitions)

            partitions.forEach { partition ->
                val position = consumer.position(partition)
                log.info("  → {}: offset={}", partition, position)
            }
        }

        override fun onPartitionsLost(consumer: Consumer<*, *>, partitions: Collection<TopicPartition>) {
            log.error("🚨 파티션 손실 (급작스러운 revoke): {}", partitions)
        }
    }
}
