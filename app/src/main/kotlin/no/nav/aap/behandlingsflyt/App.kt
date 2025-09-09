package no.nav.aap.behandlingsflyt

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
import no.nav.aap.behandlingsflyt.api.actuator.actuator
import no.nav.aap.behandlingsflyt.api.config.definisjoner.configApi
import no.nav.aap.behandlingsflyt.auditlog.auditlogApi
import no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_7.aktivitetsplikt11_7GrunnlagApi
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
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore.overgangUforeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid.overgangArbeidGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid.overgangArbeidGrunnlagApi
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
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.rettighetsperiodeGrunnlagAPI
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
import no.nav.aap.behandlingsflyt.hendelse.mottattHendelseApi
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.pip.behandlingsflytPip
import no.nav.aap.behandlingsflyt.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.behandlingsflyt.test.opprettDummySakApi
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
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

fun utledSubtypesTilMottattHendelseDTO(): List<Class<*>> {
    return Innsending::class.sealedSubclasses.map { it.java }.toList()
}

class App

private const val ANTALL_WORKERS = 4

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil av type ${e.javaClass}.", e)
        prometheus.uhåndtertExceptionTeller(e::class.java.name).increment()
    }
    embeddedServer(Netty, configure = {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
        shutdownGracePeriod = TimeUnit.SECONDS.toMillis(5)
        shutdownTimeout = TimeUnit.SECONDS.toMillis(10)
        connector {
            port = 8080
        }
    }) { server(DbConfig(), postgresRepositoryRegistry, defaultGatewayProvider()) }.start(wait = true)
}

internal fun Application.server(
    dbConfig: DbConfig,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypesTilAvklaringsbehovLøsning() + utledSubtypesTilMottattHendelseDTO())

    commonKtorModule(
        prometheus,
        AzureConfig(),
        InfoModel(
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
    val motor = startMotor(dataSource, repositoryRegistry, gatewayProvider)

    if (!Miljø.erLokal()) {
        startKabalKonsument(dataSource, repositoryRegistry)
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
                institusjonAPI(dataSource, repositoryRegistry, gatewayProvider)
                avklaringsbehovApi(dataSource, repositoryRegistry, gatewayProvider)
                tilkjentYtelseAPI(dataSource, repositoryRegistry)
                foreslaaVedtakAPI(dataSource, repositoryRegistry)
                trukketSøknadGrunnlagAPI(dataSource, repositoryRegistry)
                rettighetsperiodeGrunnlagAPI(dataSource, repositoryRegistry, gatewayProvider)
                beregningVurderingAPI(dataSource, repositoryRegistry, gatewayProvider)
                beregningsGrunnlagApi(dataSource, repositoryRegistry)
                aldersGrunnlagApi(dataSource, repositoryRegistry)
                barnetilleggApi(dataSource, repositoryRegistry, gatewayProvider)
                motorApi(dataSource)
                behandlingsflytPip(dataSource, repositoryRegistry)
                auditlogApi(dataSource, repositoryRegistry)
                refusjonGrunnlagAPI(dataSource, repositoryRegistry, gatewayProvider)
                manglendeGrunnlagApi(dataSource, repositoryRegistry)
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

fun Application.startMotor(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
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
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("Forbereder stopp av applikasjon, stopper motor.")
        motor.stop()
    }
    monitor.subscribe(ApplicationStopping) { application ->
        application.environment.log.info("Server stopper...")
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet.")
    }

    return motor
}

fun Application.startKabalKonsument(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
): KafkaKonsument {
    val konsument = KabalKafkaKonsument(
        config = KafkaConsumerConfig(),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry
    )
    monitor.subscribe(ApplicationStarted) {
        val t = Thread() {
            konsument.konsumer()
        }
        t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            log.error("Konsumering av $KABAL_EVENT_TOPIC ble lukket pga uhåndtert feil", e)
        }
        t.start()
    }
    monitor.subscribe(ApplicationStopped) {
        log.info("Applikasjonen er stoppet, lukker KabalKafkaKonsument.")
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

fun initDatasource(dbConfig: DbConfig): DataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    connectionTestQuery = "SELECT 1"
    metricRegistry = prometheus
})
