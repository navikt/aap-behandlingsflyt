package no.nav.aap.behandlingsflyt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.aap.behandlingsflyt.api.actuator.actuator
import no.nav.aap.behandlingsflyt.api.config.definisjoner.configApi
import no.nav.aap.behandlingsflyt.auditlog.auditlogApi
import no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_7.aktivitetsplikt11_7GrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.aktivitetsplikt.brudd_11_9.aktivitetsplikt11_9GrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.arbeidsevne.arbeidsevneGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.arbeidsopptrapping.arbeidsopptrappingGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.fatteVedtakGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.utledSubtypesTilAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.barnepensjon.barnepensjonGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.barnetilleggApi
import no.nav.aap.behandlingsflyt.behandling.bekreftvurderingeroppfølging.bekreftVurderingerOppfølgingApi
import no.nav.aap.behandlingsflyt.behandling.beregning.beregningsGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder.aldersGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt.meldepliktsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon.refusjonGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid.overgangArbeidGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangufore.overgangUforeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.sykdomsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag.sykepengerGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt.manglendeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt.beregningVurderingApi
import no.nav.aap.behandlingsflyt.behandling.brev.sykdomsvurderingForBrevApi
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.etableringEgenVirksomhetApi
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.foreslaaVedtakApi
import no.nav.aap.behandlingsflyt.behandling.foreslåvedtak.foreslaaVedtakVedtakslengdeApi
import no.nav.aap.behandlingsflyt.behandling.grunnlag.medlemskap.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.inntektsbortfallGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.institusjonApi
import no.nav.aap.behandlingsflyt.behandling.klage.behandlendeenhet.behandlendeEnhetGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.formkrav.formkravGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.fullmektig.fullmektigGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling.klagebehandlingKontorGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.klagebehandling.klagebehandlingNayGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling.påklagetBehandlingGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.resultat.klageresultatApi
import no.nav.aap.behandlingsflyt.behandling.klage.trekk.trekkKlageGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringApi
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringTilgangApi
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.forutgåendeMedlemskapApi
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.lovvalgMedlemskapGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.lovvalgMedlemskapApi
import no.nav.aap.behandlingsflyt.behandling.meldekort.meldekortApi
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.mellomlagretVurderingApi
import no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling.avklarOppfolgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppfolgingsbehandling.oppfølgingsOppgaveApi
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.oppholdskravGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.rettighet.rettighetsinfoApi
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.rettighetsperiodeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.revurdering.avbrytRevurderingGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.simulering.simuleringApi
import no.nav.aap.behandlingsflyt.behandling.student.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.student.sykestipend.sykestipendGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans.svarFraAndreinstansGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.søknad.trukketSøknadGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.tidligerevurderinger.tidligereVurderingerApi
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilkjentYtelseApi
import no.nav.aap.behandlingsflyt.behandling.underveis.meldepliktOverstyringGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.underveis.underveisVurderingerApi
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.vedtakslengdeGrunnlagApi
import no.nav.aap.behandlingsflyt.drift.driftApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.flyt.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flytApi
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.foreldrepenger.ForeldrepengevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.inst2.InstitusjonsOppholdKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KabalKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.sykepenger.SykepengevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.tilbakekreving.TilbakekrevingKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.uføre.UførevedtakKafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.mottattHendelseApi
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.InstitusjonsOppholdHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.pip.behandlingsflytPipApi
import no.nav.aap.behandlingsflyt.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeBackfillMigrering
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.behandlingsflyt.test.fullførBehandlingApi
import no.nav.aap.behandlingsflyt.test.opprettDummySakApi
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.server.plugins.NavIdentInterceptor
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import no.nav.aap.tilgang.TilgangGateway
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
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

    const val ANTALL_WORKERS_FOR_MOTOR = 4

    // Vi følger *IKKE* ktor sin metodikk for å regne ut callGroupSize, for den metodikken antar at
    // handlerene våre gjør async IO, men vi gjør ikke async IO, hverken mot database eller i HTTP-kall.
    const val callGroupSize = 64

    /* praktisk talt alle endepunkt hos oss starter med en transaksjon. Siden transaksjonen
     * er blocking, er det i praksis hva som begrenser antall parallelle kall.
     *
     * Vi har maks 100 connections i prod, og vi kjører med 3-4 pods, så det er en begrenset ressurs.
     */
    const val hikariMaxPoolSize = 100 / 4 /* max connections / max antall pods */
}

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil av type ${e.javaClass}.", e)
        prometheus.uhåndtertExceptionTeller(e::class.java.name).increment()
    }

    aktiverPostgresLogging()

    embeddedServer(Netty, configure = {
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
    prometheus: PrometheusMeterRegistry = no.nav.aap.behandlingsflyt.prometheus,
) {
    DefaultJsonMapper.objectMapper()
        .registerSubtypes(utledSubtypesTilAvklaringsbehovLøsning() + utledSubtypesTilMottattHendelseDTO())

    val unleashGateway: UnleashGateway = gatewayProvider.provide()
    if (unleashGateway.isEnabled(BehandlingsflytFeature.GJustering2026) && Miljø.erDev()) {
        Grunnbeløp.aktiverGJustering2026()
    }

    commonKtorModule(
        prometheus = prometheus,
        infoModel = InfoModel(
            title = "AAP - Behandlingsflyt",
            version = ApplikasjonsVersjon.versjon,
            description = """
            For å teste API i dev, besøk
            <a href="https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:behandlingsflyt">Token Generator</a> for å få token.
            """.trimIndent(),
        ),
        identityProvider = IdentityProvider.ENTRA_ID
    )

    install(StatusPages, StatusPagesConfigHelper.setup())

    val dedicatedMotorConnections = AppConfig.ANTALL_WORKERS_FOR_MOTOR * 2
    val fellesDataSource = initDatasource(
        dbConfig,
        maximumPoolSize = AppConfig.hikariMaxPoolSize - dedicatedMotorConnections,
        prometheus = prometheus,
    )
    val motorDataSource = initDatasource(
        dbConfig,
        maximumPoolSize = dedicatedMotorConnections,
        prometheus = prometheus,
    )
    Migrering.migrate(fellesDataSource)

    val scheduler = utførMigreringer(fellesDataSource, gatewayProvider, environment.log)

    val motor = startMotor(motorDataSource, repositoryRegistry, gatewayProvider, prometheus)

    startKafkakonsumenter(fellesDataSource, repositoryRegistry, gatewayProvider)
    TilgangGateway.initialiserPrometheus(prometheus)

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
            fellesDataSource.close()
            motorDataSource.close()
        } catch (_: Exception) {
            // Ignorert
        }
    }

    routing {
        authenticate(IdentityProvider.ENTRA_ID.value) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                personApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                saksApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                behandlingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                flytApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                fatteVedtakGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                kvalitetssikringApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                kvalitetssikringTilgangApi(fellesDataSource, repositoryRegistry)
                bistandsgrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                meldepliktsgrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                meldepliktOverstyringGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                vedtakslengdeGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                arbeidsevneGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                arbeidsopptrappingGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                etableringEgenVirksomhetApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                overgangUforeGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                medlemskapsgrunnlagApi(fellesDataSource, repositoryRegistry)
                studentgrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                sykestipendGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                sykdomsgrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                sykdomsvurderingForBrevApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                sykepengerGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                inntektsbortfallGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                oppholdskravGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                institusjonApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                avklaringsbehovApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                tilkjentYtelseApi(fellesDataSource, repositoryRegistry)
                foreslaaVedtakApi(fellesDataSource, repositoryRegistry)
                foreslaaVedtakVedtakslengdeApi(fellesDataSource, repositoryRegistry)
                trukketSøknadGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                avbrytRevurderingGrunnlagApi(fellesDataSource, repositoryRegistry)
                rettighetsperiodeGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                beregningVurderingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                beregningsGrunnlagApi(fellesDataSource, repositoryRegistry)
                aldersGrunnlagApi(fellesDataSource, repositoryRegistry)
                barnetilleggApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                motorApi(fellesDataSource)
                behandlingsflytPipApi(fellesDataSource, repositoryRegistry)
                auditlogApi(fellesDataSource, repositoryRegistry)
                refusjonGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                manglendeGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                mellomlagretVurderingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                rettighetsinfoApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                tidligereVurderingerApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                barnepensjonGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                bekreftVurderingerOppfølgingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                // Klage
                påklagetBehandlingGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                fullmektigGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                formkravGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                behandlendeEnhetGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                klagebehandlingKontorGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                klagebehandlingNayGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                klageresultatApi(fellesDataSource, repositoryRegistry)
                trekkKlageGrunnlagApi(fellesDataSource, repositoryRegistry)
                // Svar fra kabal
                svarFraAndreinstansGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                // Oppfølgingsbehandling
                avklarOppfolgingsoppgaveGrunnlag(fellesDataSource, repositoryRegistry)
                oppfølgingsOppgaveApi(fellesDataSource, repositoryRegistry)
                // Aktivitetsplikt
                aktivitetsplikt11_7GrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                aktivitetsplikt11_9GrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                // Meldekort
                meldekortApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                // Flytt
                brevApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                dokumentinnhentingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                mottattHendelseApi(fellesDataSource, repositoryRegistry)
                underveisVurderingerApi(fellesDataSource, repositoryRegistry)
                lovvalgMedlemskapApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                lovvalgMedlemskapGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                samordningGrunnlag(fellesDataSource, repositoryRegistry, gatewayProvider)
                forutgåendeMedlemskapApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                driftApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                simuleringApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                overgangArbeidGrunnlagApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                // Endepunkter kun tilgjengelig lokalt og i test
                if (!Miljø.erProd()) {
                    opprettDummySakApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                    fullførBehandlingApi(fellesDataSource, repositoryRegistry, gatewayProvider)
                }
            }
        }
        actuator(prometheus, motor)
    }

}

private fun Application.startKafkakonsumenter(
    dataSource: HikariDataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    if (!Miljø.erLokal()) {
        startKonsument(
            KabalKafkaKonsument(
                config = KafkaConsumerConfig(),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout
            )
        )
        startKonsument(
            PdlHendelseKafkaKonsument(
                config = KafkaConsumerConfig(
                    valueDeserializer = KafkaAvroDeserializer::class.java
                ),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout,
                gatewayProvider = gatewayProvider
            )
        )
        startKonsument(
            TilbakekrevingKafkaKonsument(
                config = KafkaConsumerConfig(),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout
            )
        )
        startKonsument(
            SykepengevedtakKafkaKonsument(
                config = KafkaConsumerConfig(),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout,
                gatewayProvider = gatewayProvider
            )
        )
        startKonsument(
            InstitusjonsOppholdKafkaKonsument(
                config = KafkaConsumerConfig(
                    valueDeserializer = JsonDeserializerInstitusjonsOppholdHendelse::class.java,
                ),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout,
                gatewayProvider = gatewayProvider,
                institusjonsoppholdKlient = InstitusjonsoppholdGatewayImpl
            )
        )
        startKonsument(
            UførevedtakKafkaKonsument(
                config = KafkaConsumerConfig(),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout,
                gatewayProvider = gatewayProvider
            )
        )
        startKonsument(
            ForeldrepengevedtakKafkaKonsument(
                config = KafkaConsumerConfig(),
                dataSource = dataSource,
                repositoryRegistry = repositoryRegistry,
                closeTimeout = AppConfig.stansArbeidTimeout,
                gatewayProvider = gatewayProvider
            )
        )
    }
}

private fun <K, V> Application.startKonsument(konsument: KafkaKonsument<K, V>) {
    monitor.subscribe(ApplicationStarted) {
        val t = Thread {
            konsument.konsumer()
        }
        t.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            log.error("Konsumering av ${konsument.topic} ble lukket pga uhåndtert feil", e)
        }
        t.start()
    }
    monitor.subscribe(ApplicationStopping) { env ->
        // ktor sine eventer kjøres synkront, så vi må kjøre dette asynkront for ikke å blokkere nedstengings-sekvensen
        env.launch(Dispatchers.IO) {
            konsument.lukk()
        }
    }
}

// Bruker leaderElector for å sikre at kun en pod kjører migreringen og spinner opp en egen tråd for å ikke blokkere.
private fun utførMigreringer(
    dataSource: HikariDataSource,
    gatewayProvider: GatewayProvider,
    log: io.ktor.util.logging.Logger
): ScheduledExecutorService {
    val scheduler = Executors.newScheduledThreadPool(1)
    /* Prøv på nytt, for å se om vi er elected til leader, hvert 9. minutt. Hvis vi blir elected, så vil metoden
     * aldri returnere, og med fixed delay, så blir det heller ikke skjedulert flere tasks.
    **/
    scheduler.scheduleWithFixedDelay({
        val unleashGateway: UnleashGateway = gatewayProvider.provide()
        val isLeader = isLeader(log)
        log.info("isLeader = $isLeader")

        if (unleashGateway.isEnabled(BehandlingsflytFeature.BackfillYrkesskadeNyeFelter) && isLeader) {
            YrkesskadeBackfillMigrering(dataSource, postgresRepositoryRegistry, gatewayProvider).migrer()
        }

    }, 1, 9, TimeUnit.MINUTES)
    return scheduler
}

fun Application.startMotor(
    dataSource: HikariDataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    prometheus: PrometheusMeterRegistry = no.nav.aap.behandlingsflyt.prometheus,
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

fun initDatasource(
    dbConfig: DbConfig,
    maximumPoolSize: Int = AppConfig.hikariMaxPoolSize,
    prometheus: PrometheusMeterRegistry = no.nav.aap.behandlingsflyt.prometheus,
): HikariDataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    dataSourceProperties = postgresConfig
    this.maximumPoolSize = maximumPoolSize
    minimumIdle = 1
    connectionTestQuery = "SELECT 1"
    metricRegistry = prometheus
})

class JsonDeserializerInstitusjonsOppholdHendelse : Deserializer<InstitusjonsOppholdHendelseKafkaMelding> {
    private val mapper = jacksonObjectMapper()

    override fun deserialize(
        topic: String?,
        data: ByteArray,
    ): InstitusjonsOppholdHendelseKafkaMelding = mapper.readValue(data)
}