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
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.avklaringsbehovApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.fatteVedtakGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.utledSubtypesTilAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.flate.barnetilleggApi
import no.nav.aap.behandlingsflyt.behandling.beregning.beregningsGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder.aldersGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt.meldepliktsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon.refusjonGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.sykdomsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykepengergrunnlag.sykepengerGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.manuellinntekt.manglendeGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt.beregningVurderingAPI
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.aktivitetspliktApi
import no.nav.aap.behandlingsflyt.behandling.etannetsted.institusjonAPI
import no.nav.aap.behandlingsflyt.behandling.grunnlag.medlemskap.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.behandling.klage.effektueravvistpåformkrav.effektuerAvvistPåFormkravGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.klage.resultat.klageresultatApi
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringApi
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.forutgåendeMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.lovvalgMedlemskapGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.lovvalgMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.rettighetsperiodeGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.simulering.simuleringAPI
import no.nav.aap.behandlingsflyt.behandling.søknad.trukketSøknadGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilkjentYtelseAPI
import no.nav.aap.behandlingsflyt.behandling.underveis.underveisVurderingerAPI
import no.nav.aap.behandlingsflyt.drift.driftAPI
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.flate.behandlendeEnhetGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate.formkravGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.flate.klagebehandlingKontorGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.flate.klagebehandlingNayGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.flate.påklagetBehandlingGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.arbeidsevneGrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.behandling.student.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.flyt.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flytApi
import no.nav.aap.behandlingsflyt.hendelse.mottattHendelseApi
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.ApiInternGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.SamGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.behandling.klage.trekk.trekkKlageGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans.svarFraAndreinstansGrunnlagApi
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaConsumerConfig
import no.nav.aap.behandlingsflyt.hendelse.kafka.KafkaKonsument
import no.nav.aap.behandlingsflyt.hendelse.kafka.klage.KabalKafkaKonsument
import no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold.EREGGateway
import no.nav.aap.behandlingsflyt.integrasjon.kabal.KabalGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.tilgang.TilgangGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.unleash.UnleashGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.pip.behandlingsflytPip
import no.nav.aap.behandlingsflyt.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayRegistry
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
        LoggerFactory.getLogger(App::class.java).error("Uhåndtert feil.", e)
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
    }) { server(DbConfig(), postgresRepositoryRegistry) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig, repositoryRegistry: RepositoryRegistry) {
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
    val motor = startMotor(dataSource, repositoryRegistry)
    registerGateways()

    if (Miljø.erDev()) {
        startKabalKonsument(dataSource, repositoryRegistry)
    }

    routing {
        authenticate(AZURE) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                saksApi(dataSource, repositoryRegistry)
                behandlingApi(dataSource, repositoryRegistry)
                flytApi(dataSource, repositoryRegistry)
                fatteVedtakGrunnlagApi(dataSource, repositoryRegistry)
                kvalitetssikringApi(dataSource, repositoryRegistry)
                bistandsgrunnlagApi(dataSource, repositoryRegistry)
                meldepliktsgrunnlagApi(dataSource, repositoryRegistry)
                arbeidsevneGrunnlagApi(dataSource, repositoryRegistry)
                medlemskapsgrunnlagApi(dataSource, repositoryRegistry)
                studentgrunnlagApi(dataSource, repositoryRegistry)
                sykdomsgrunnlagApi(dataSource, repositoryRegistry)
                sykepengerGrunnlagApi(dataSource, repositoryRegistry)
                institusjonAPI(dataSource, repositoryRegistry)
                avklaringsbehovApi(dataSource, repositoryRegistry)
                tilkjentYtelseAPI(dataSource, repositoryRegistry)
                trukketSøknadGrunnlagAPI(dataSource, repositoryRegistry)
                rettighetsperiodeGrunnlagAPI(dataSource, repositoryRegistry)
                beregningVurderingAPI(dataSource, repositoryRegistry)
                beregningsGrunnlagApi(dataSource, repositoryRegistry)
                aldersGrunnlagApi(dataSource, repositoryRegistry)
                barnetilleggApi(dataSource, repositoryRegistry)
                motorApi(dataSource)
                behandlingsflytPip(dataSource, repositoryRegistry)
                aktivitetspliktApi(dataSource, repositoryRegistry)
                auditlogApi(dataSource, repositoryRegistry)
                refusjonGrunnlagAPI(dataSource, repositoryRegistry)
                manglendeGrunnlagApi(dataSource, repositoryRegistry)
                //Klage
                påklagetBehandlingGrunnlagApi(dataSource, repositoryRegistry)
                formkravGrunnlagApi(dataSource, repositoryRegistry)
                behandlendeEnhetGrunnlagApi(dataSource, repositoryRegistry)
                klagebehandlingKontorGrunnlagApi(dataSource, repositoryRegistry)
                klagebehandlingNayGrunnlagApi(dataSource, repositoryRegistry)
                klageresultatApi(dataSource, repositoryRegistry)
                trekkKlageGrunnlagAPI(dataSource, repositoryRegistry)
                effektuerAvvistPåFormkravGrunnlagApi(dataSource, repositoryRegistry)
                // Svar fra kabal
                svarFraAndreinstansGrunnlagApi(dataSource, repositoryRegistry)
                // Flytt
                brevApi(dataSource, repositoryRegistry)
                dokumentinnhentingAPI(dataSource, repositoryRegistry)
                mottattHendelseApi(dataSource, repositoryRegistry)
                underveisVurderingerAPI(dataSource, repositoryRegistry)
                lovvalgMedlemskapAPI(dataSource, repositoryRegistry)
                lovvalgMedlemskapGrunnlagAPI(dataSource, repositoryRegistry)
                samordningGrunnlag(dataSource, repositoryRegistry)
                forutgåendeMedlemskapAPI(dataSource, repositoryRegistry)
                driftAPI(dataSource, repositoryRegistry)
                simuleringAPI(dataSource, repositoryRegistry)
            }
        }
        actuator(prometheus, motor)
    }

}

private fun registerGateways() {
    GatewayRegistry.register<PdlBarnGateway>()
        .register<PdlIdentGateway>()
        .register<PdlPersoninfoBulkGateway>()
        .register<PdlPersoninfoGateway>()
        .register<AbakusForeldrepengerGateway>()
        .register<AbakusSykepengerGateway>()
        .register<DokumentinnhentingGatewayImpl>()
        .register<MedlemskapGateway>()
        .register<ApiInternGatewayImpl>()
        .register<UtbetalingGatewayImpl>()
        .register<AARegisterGateway>()
        .register<EREGGateway>()
        .register<StatistikkGatewayImpl>()
        .register<InntektGatewayImpl>()
        .register<BrevGateway>()
        .register<OppgavestyringGatewayImpl>()
        .register<UføreGateway>()
        .register<YrkesskadeRegisterGatewayImpl>()
        .register<MeldekortGatewayImpl>()
        .register<TilgangGatewayImpl>()
        .register<TjenestePensjonGatewayImpl>()
        .register<UnleashGatewayImpl>()
        .register<SamGatewayImpl>()
        .register<NomInfoGateway>()
        .register<NorgGateway>()
        .register<KabalGateway>()
        .status()
}

fun Application.startMotor(dataSource: DataSource, repositoryRegistry: RepositoryRegistry): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = BehandlingsflytLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = prometheus,
        repositoryRegistry = repositoryRegistry,
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
): KafkaKonsument? {
    val konsument = KabalKafkaKonsument(
        config = KafkaConsumerConfig(),
        dataSource = dataSource,
        repositoryRegistry = repositoryRegistry
    )
    monitor.subscribe(ApplicationStarted) {
        Thread {
            konsument.konsumer()
        }.start()
    }
    monitor.subscribe(ApplicationStopped) {
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
