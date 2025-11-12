package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.aap.behandlingsflyt.api.actuator.actuator
import no.nav.aap.behandlingsflyt.api.config.definisjoner.configApi
import no.nav.aap.behandlingsflyt.auditlog.auditlogApi
import no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_7.aktivitetsplikt11_7GrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_9.aktivitetsplikt11_9GrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.arbeidsevne.arbeidsevneGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.fatteVedtakGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.utledSubtypesTilAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.barnetilleggApi
import no.nav.aap.behandlingsflyt.behandling.beregning.beregningsGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder.aldersGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt.meldepliktsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon.refusjonGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid.overgangArbeidGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore.overgangUforeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.sykdomsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag.sykepengerGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt.manglendeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt.beregningVurderingAPI
import no.nav.aap.behandlingsflyt.behandling.brev.sykdomsvurderingForBrevApi
import no.nav.aap.behandlingsflyt.behandling.etannetsted.institusjonAPI
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.foreslaaVedtakAPI
import no.nav.aap.behandlingsflyt.behandling.grunnlag.medlemskap.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.behandling.klage.behandlendeenhet.behandlendeEnhetGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.formkrav.formkravGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.fullmektig.fullmektigGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling.klagebehandlingKontorGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling.klagebehandlingNayGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling.påklagetBehandlingGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.resultat.klageresultatApi
import no.nav.aap.behandlingsflyt.behandling.klage.trekk.trekkKlageGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringApi
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringTilgangAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.forutgåendeMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.lovvalgMedlemskapGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.lovvalgMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.mellomlagretVurderingApi
import no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling.avklarOppfolgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling.oppfølgingsOppgaveApi
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.oppholdskravGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.rettighetsperiodeGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.revurdering.avbrytRevurderingGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.simulering.simuleringAPI
import no.nav.aap.behandlingsflyt.behandling.student.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans.svarFraAndreinstansGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.søknad.trukketSøknadGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilkjentYtelseAPI
import no.nav.aap.behandlingsflyt.behandling.underveis.meldepliktOverstyringGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.underveis.underveisVurderingerAPI
import no.nav.aap.behandlingsflyt.drift.driftAPI
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.flyt.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flytApi
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KABAL_EVENT_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KabalKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PDL_HENDELSE_TOPIC
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottattHendelseApi
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.pip.behandlingsflytPip
import no.nav.aap.behandlingsflyt.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.behandlingsflyt.test.opprettDummySakApi
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.server.plugins.NavIdentInterceptor
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.person.pdl.leesah.Personhendelse
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds


fun utledSubtypesTilMottattHendelseDTO(): List<Class<*>> {
    return Innsending::class.sealedSubclasses.map { it.java }.toList()
}

class App

internal object AppConfig {
    // Matcher terminationGracePeriodSeconds for podden i Kubernetes-manifestet ("nais.yaml")
    private val kubernetesTimeout = 30.seconds

    // Tid før ktor avslutter uansett. Må være litt mindre enn `kubernetesTimeout`.
    val shutdownTimeout = kubernetesTimeout - 2.seconds

    // Tid appen får til å fullføre påbegynte requests, jobber etc. Må være mindre enn `endeligShutdownTimeout`.
    val shutdownGracePeriod = shutdownTimeout - 3.seconds

    // Tid appen får til å avslutte Motor, Kafka, etc
    val stansArbeidTimeout = shutdownGracePeriod - 1.seconds

    // Vi skrur opp ktor sin default-verdi, som er "antall CPUer", fordi vi har en del venting på IO (db, kafka, http):
    private val ktorParallellitet = 8
    // Vi følger ktor sin metodikk for å regne ut tuning parametre som funksjon av parallellitet
    // https://github.com/ktorio/ktor/blob/3.3.1/ktor-server/ktor-server-core/common/src/io/ktor/server/engine/ApplicationEngine.kt#L30
    val connectionGroupSize = ktorParallellitet / 2 + 1
    val workerGroupSize = ktorParallellitet / 2 + 1
    val callGroupSize = ktorParallellitet

    const val ANTALL_WORKERS_FOR_MOTOR = 4
    val hikariMaxPoolSize = ktorParallellitet + 2 * ANTALL_WORKERS_FOR_MOTOR
}

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil av type ${e.javaClass}.", e)
        prometheus.uhåndtertExceptionTeller(e::class.java.name).increment()
    }

    aktiverPostgresLogging()

    embeddedServer(Netty, configure = {
        connectionGroupSize = AppConfig.connectionGroupSize
        workerGroupSize = AppConfig.workerGroupSize
        callGroupSize = AppConfig.callGroupSize

        shutdownGracePeriod = AppConfig.shutdownGracePeriod.inWholeMilliseconds
        shutdownTimeout = AppConfig.shutdownTimeout.inWholeMilliseconds
        connector {
            port = 8080
        }
    }) { server(DbConfig(), postgresRepositoryRegistry, defaultGatewayProvider()) }.start(wait = true)
}

private fun aktiverPostgresLogging() {
    // Basert på on https://www.baeldung.com/java-jul-to-slf4j-bridge#1-programmatic-configuration
    SLF4JBridgeHandler.install()
    // Overrider log level fra postgres-jdbc's default, som ikke logger noe som helst
    Logger.getLogger("org.postgresql").level = Level.WARNING // minste-nivå
    // Vi kan fra nå av bruke org.postgresql loggeren i logback.xml
}

internal fun Application.server(
    dbConfig: DbConfig,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypesTilAvklaringsbehovLøsning() + utledSubtypesTilMottattHendelseDTO())

    commonKtorModule(
        prometheus, AzureConfig(), InfoModel(
            title = "AAP - Behandlingsflyt", version = ApplikasjonsVersjon.versjon,
            description = """
                For å teste API i dev, besøk
                <a href="https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:behandlingsflyt">Token Generator</a> for å få token.
                """.trimIndent(),
        )
    )

    install(StatusPages, StatusPagesConfigHelper.setup())

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    val dataSource = initDatasource(dbConfig)
    Migrering.migrate(dataSource)

    val scheduler = utførMigreringer(dataSource, gatewayProvider, environment.log)

    val motor = startMotor(dataSource, repositoryRegistry, gatewayProvider)

    if (!Miljø.erLokal()) {
        startKabalKonsument(dataSource, repositoryRegistry)

    }
    if (Miljø.erDev()) {
        startPDLHendelseKonsument(dataSource, repositoryRegistry, gatewayProvider)
    }

    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { environment ->
        environment.log.info("ktor stopper nå å ta imot nye requester, og lar mottatte requester kjøre frem til timeout.")
    }
    monitor.subscribe(ApplicationStopped) { environment ->
        environment.log.info("ktor har fullført nedstoppingen sin. Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt.")
        try {
            scheduler.shutdownNow()
            // Helt til slutt, nå som vi har stanset Motor, etc. Lukk database-koblingen.
            dataSource.close()
        } catch (_: Exception) {
            // Ignorert
        }
    }

    routing {
        authenticate(AZURE) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                saksApi(dataSource, repositoryRegistry, gatewayProvider)
                behandlingApi(dataSource, repositoryRegistry, gatewayProvider)
                flytApi(dataSource, repositoryRegistry, gatewayProvider)
                fatteVedtakGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                kvalitetssikringApi(dataSource, repositoryRegistry, gatewayProvider)
                kvalitetssikringTilgangAPI(dataSource, repositoryRegistry)
                bistandsgrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                meldepliktsgrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                meldepliktOverstyringGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                arbeidsevneGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                overgangUforeGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                medlemskapsgrunnlagApi(dataSource, repositoryRegistry)
                studentgrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                sykdomsgrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                sykdomsvurderingForBrevApi(dataSource, repositoryRegistry, gatewayProvider)
                sykepengerGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                oppholdskravGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                institusjonAPI(dataSource, repositoryRegistry, gatewayProvider)
                avklaringsbehovApi(dataSource, repositoryRegistry, gatewayProvider)
                tilkjentYtelseAPI(dataSource, repositoryRegistry)
                foreslaaVedtakAPI(dataSource, repositoryRegistry)
                trukketSøknadGrunnlagAPI(dataSource, repositoryRegistry)
                avbrytRevurderingGrunnlagAPI(dataSource, repositoryRegistry)
                rettighetsperiodeGrunnlagAPI(dataSource, repositoryRegistry, gatewayProvider)
                beregningVurderingAPI(dataSource, repositoryRegistry, gatewayProvider)
                beregningsGrunnlagApi(dataSource, repositoryRegistry)
                aldersGrunnlagApi(dataSource, repositoryRegistry)
                barnetilleggApi(dataSource, repositoryRegistry, gatewayProvider)
                motorApi(dataSource)
                behandlingsflytPip(dataSource, repositoryRegistry)
                auditlogApi(dataSource, repositoryRegistry)
                refusjonGrunnlagAPI(dataSource, repositoryRegistry, gatewayProvider)
                manglendeGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                mellomlagretVurderingApi(dataSource, repositoryRegistry)
                // Klage
                påklagetBehandlingGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                fullmektigGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                formkravGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                behandlendeEnhetGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                klagebehandlingKontorGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                klagebehandlingNayGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                klageresultatApi(dataSource, repositoryRegistry)
                trekkKlageGrunnlagAPI(dataSource, repositoryRegistry)
                // Svar fra kabal
                svarFraAndreinstansGrunnlagApi(dataSource, repositoryRegistry)
                // Oppfølgingsbehandling
                avklarOppfolgingsoppgaveGrunnlag(dataSource, repositoryRegistry)
                oppfølgingsOppgaveApi(dataSource, repositoryRegistry)
                // Aktivitetsplikt
                aktivitetsplikt11_7GrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                aktivitetsplikt11_9GrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                // Flytt
                brevApi(dataSource, repositoryRegistry, gatewayProvider)
                dokumentinnhentingAPI(dataSource, repositoryRegistry, gatewayProvider)
                mottattHendelseApi(dataSource, repositoryRegistry, gatewayProvider)
                underveisVurderingerAPI(dataSource, repositoryRegistry)
                lovvalgMedlemskapAPI(dataSource, repositoryRegistry)
                lovvalgMedlemskapGrunnlagAPI(dataSource, repositoryRegistry, gatewayProvider)
                samordningGrunnlag(dataSource, repositoryRegistry, gatewayProvider)
                forutgåendeMedlemskapAPI(dataSource, repositoryRegistry, gatewayProvider)
                driftAPI(dataSource, repositoryRegistry, gatewayProvider)
                simuleringAPI(dataSource, repositoryRegistry, gatewayProvider)
                overgangArbeidGrunnlagApi(dataSource, repositoryRegistry, gatewayProvider)
                // Endepunkter kun tilgjengelig lokalt og i test
                if (!Miljø.erProd()) {
                    opprettDummySakApi(dataSource, repositoryRegistry, gatewayProvider)
                }
            }
        }
        actuator(prometheus, motor)
    }

}

// Bruker leaderElector for å sikre at kun en pod kjører migreringen og spinner opp en egen tråd for å ikke blokkere.
private fun utførMigreringer(
    dataSource: HikariDataSource,
    gatewayProvider: GatewayProvider,
    log: io.ktor.util.logging.Logger
): ScheduledExecutorService {
    val scheduler = Executors.newScheduledThreadPool(1)

    scheduler.schedule(Runnable {
        val unleashGateway: UnleashGateway = gatewayProvider.provide()
        val isLeader = isLeader(log)
        log.info("isLeader = $isLeader")
    }, 9, TimeUnit.MINUTES)
    return scheduler
}

private fun isLeader(log: io.ktor.util.logging.Logger): Boolean {
    val electorUrl = requiredConfigForKey("elector.get.url")
    val client = HttpClient.newHttpClient()
    val response = client.send(
        HttpRequest.newBuilder().uri(URI.create(electorUrl)).GET().build(),
        HttpResponse.BodyHandlers.ofString()
    )
    val json = ObjectMapper().readTree(response.body())
    val leaderHostname = json.get("name").asText()
    val hostname = InetAddress.getLocalHost().hostName
    log.info("electorUrl=${electorUrl}, leaderHostname=$leaderHostname, hostname=$hostname")
    return hostname == leaderHostname
}

fun Application.startMotor(
    dataSource: HikariDataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = AppConfig.ANTALL_WORKERS_FOR_MOTOR,
        logInfoProvider = BehandlingsflytLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = prometheus,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = gatewayProvider,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopping) { env ->
        // ktor sine eventer kjøres synkront, så vi må kjøre dette asynkront for ikke å blokkere nedstengings-sekvensen
        env.launch(Dispatchers.IO) {
            motor.stop(AppConfig.stansArbeidTimeout)
        }
    }

    return motor
}

fun Application.startKabalKonsument(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry
): KafkaKonsument<String, String> {
    val konsument = KabalKafkaKonsument(
        config = KafkaConsumerConfig(), dataSource = dataSource, repositoryRegistry = repositoryRegistry
    )
    monitor.subscribe(ApplicationStarted) {
        val t = Thread {
            konsument.konsumer()
        }
        t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            log.error("Konsumering av $KABAL_EVENT_TOPIC ble lukket pga uhåndtert feil", e)
        }
        t.start()
    }
    monitor.subscribe(ApplicationStopping) { env ->
        // ktor sine eventer kjøres synkront, så vi må kjøre dette asynkront for ikke å blokkere nedstengings-sekvensen
        env.launch(Dispatchers.IO) {
            konsument.lukk()
        }
    }

    return konsument
}

fun Application.startPDLHendelseKonsument(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
): KafkaKonsument<String, Personhendelse> {
    val konsument = PdlHendelseKafkaKonsument(
        config = KafkaConsumerConfig(
            keyDeserializer = org.apache.kafka.common.serialization.StringDeserializer::class.java,
            valueDeserializer = io.confluent.kafka.serializers.KafkaAvroDeserializer::class.java
        ),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry,
        gatewayProvider = gatewayProvider
    )
    monitor.subscribe(ApplicationStarted) {
        val t = Thread() {
            konsument.konsumer()
        }
        t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            log.error("Konsumering av $PDL_HENDELSE_TOPIC ble lukket pga uhåndtert feil", e)
        }
        t.start()
    }
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("Forbereder stopp av applikasjon, lukker PDLHendelseKonsument.")

        konsument.lukk()
    }

    return konsument
}

class DbConfig(
    val url: String = System.getenv(
        "NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_JDBC_URL"
    ),
    val username: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_USERNAME"),
    val password: String = System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_PASSWORD")
)

val postgresConfig = Properties().apply {
    put("tcpKeepAlive", true) // kreves av Hikari

    put("socketTimeout", 300) // sekunder, makstid for overføring av svaret fra db
    put("statement_timeout", 300_000) // millisekunder, makstid for db til å utføre spørring

    put("logUnclosedConnections", true) // vår kode skal lukke alle connections
    put("logServerErrorDetail", false) // ikke lekk person-data fra queries etc til logger ved feil

    put("assumeMinServerVersion", "16.0") // raskere oppstart av driver
}

fun initDatasource(dbConfig: DbConfig): HikariDataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    dataSourceProperties = postgresConfig
    maximumPoolSize = AppConfig.hikariMaxPoolSize
    minimumIdle = 1
    connectionTestQuery = "SELECT 1"
    metricRegistry = prometheus
})