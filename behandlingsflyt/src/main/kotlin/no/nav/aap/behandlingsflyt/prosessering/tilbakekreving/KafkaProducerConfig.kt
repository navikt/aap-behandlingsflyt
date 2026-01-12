package no.nav.aap.behandlingsflyt.prosessering.tilbakekreving

import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import tools.jackson.databind.ser.jdk.StringSerializer
import java.util.*

data class KafkaProducerConfig<K, V>(
    val applicationId: String = requiredConfigForKey("NAIS_APP_NAME"),
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val ssl: SslConfig? = SslConfig(),
    val schemaRegistry: SchemaRegistryConfig? = SchemaRegistryConfig(),
    val additionalProperties: Properties = Properties(),
) {
    fun producerProperties(producerName: String): Properties = Properties().apply {
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[CommonClientConfigs.CLIENT_ID_CONFIG] = "$applicationId-$producerName"
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true

        ssl?.let { putAll(it.properties()) }
        schemaRegistry?.let { putAll(it.properties()) }
        putAll(additionalProperties)

        put("specific.avro.reader", true)
    }
}


data class SchemaRegistryConfig(
    val url: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY"),
    val user: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_USER"),
    val password: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_PASSWORD")
) {
    fun properties() = Properties().apply {
        this["schema.registry.url"] = url
        this["basic.auth.credentials.source"] = "USER_INFO"
        this["basic.auth.user.info"] = "$user:$password"
    }
}

data class SslConfig(
    private val truststorePath: String = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH"),
    private val keystorePath: String = requiredConfigForKey("KAFKA_KEYSTORE_PATH"),
    private val credstorePsw: String = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD")
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