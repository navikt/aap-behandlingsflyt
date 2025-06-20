package no.nav.aap.behandlingsflyt.hendelse.kafka

import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

data class KafkaConsumerConfig(
    val applicationId: String = requiredConfigForKey("KAFKA_STREAMS_APPLICATION_ID"),
    val maxPollRecords: Int = 1,
    val autoOffsetReset: String = "earliest",
    val enableAutoConfig: Boolean = false,
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val ssl: SslConfig? = SslConfig(),
    val schemaRegistry: SchemaRegistryConfig? = SchemaRegistryConfig(),
    val compressionType: String = "snappy",
    val additionalProperties: Properties = Properties(),
) {
    fun consumerProperties(): Properties = Properties().apply {
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[CommonClientConfigs.CLIENT_ID_CONFIG] = applicationId

        ssl?.let { putAll(it.properties()) }
        schemaRegistry?.let { putAll(it.properties()) }
        putAll(additionalProperties)

        this[ConsumerConfig.GROUP_ID_CONFIG] = applicationId
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = maxPollRecords
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetReset
        this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enableAutoConfig
        this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java

    }
}

data class SslConfig(
    private val truststorePath: String = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH"),
    private val keystorePath: String = requiredConfigForKey("KAFKA_KEYSTORE_PATH"),
    private val credstorePsw: String = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD"),
) {
    fun properties() = Properties().apply {
        this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
        this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
        this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePsw
        this[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
    }
}

data class SchemaRegistryConfig(
    private val url: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY"),
    private val user: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_USER"),
    private val password: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
) {
    fun properties() = Properties().apply {
        this["schema.registry.url"] = url
        this["basic.auth.credentials.source"] = "USER_INFO"
        this["basic.auth.user.info"] = "$user:$password"
    }
}
