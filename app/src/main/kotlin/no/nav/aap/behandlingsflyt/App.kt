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
import no.nav.aap.behandlingsflyt.behandling.beregning.tidspunkt.beregningVurderingAPI
import no.nav.aap.behandlingsflyt.behandling.brev.brevApi
import no.nav.aap.behandlingsflyt.behandling.bruddaktivitetsplikt.aktivitetspliktApi
import no.nav.aap.behandlingsflyt.behandling.etannetsted.institusjonAPI
import no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning.samordningGrunnlag
import no.nav.aap.behandlingsflyt.behandling.kvalitetssikring.kvalitetssikringApi
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.forutgåendeMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag.lovvalgMedlemskapGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.lovvalgMedlemskapAPI
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.rettighetsperiodeGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.søknad.trukketSøknadGrunnlagAPI
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilkjentYtelseAPI
import no.nav.aap.behandlingsflyt.behandling.underveis.underveisVurderingerAPI
import no.nav.aap.behandlingsflyt.drift.driftAPI
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate.arbeidsevneGrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.bistandsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.medlemskap.flate.medlemskapsgrunnlagApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.flate.studentgrunnlagApi
import no.nav.aap.behandlingsflyt.flyt.behandlingApi
import no.nav.aap.behandlingsflyt.flyt.flytApi
import no.nav.aap.behandlingsflyt.hendelse.mottattHendelseApi
import no.nav.aap.behandlingsflyt.integrasjon.aaregisteret.AARegisterGateway
import no.nav.aap.behandlingsflyt.integrasjon.barn.PdlBarnGateway
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.integrasjon.datadeling.ApiInternGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting.DokumentinnhentingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoBulkGateway
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.inntekt.InntektGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.medlemsskap.MedlemskapGateway
import no.nav.aap.behandlingsflyt.integrasjon.meldekort.MeldekortGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.oppgave.OppgavestyringGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusForeldrepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.AbakusSykepengerGateway
import no.nav.aap.behandlingsflyt.integrasjon.samordning.TjenestePensjonGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.statistikk.StatistikkGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreGateway
import no.nav.aap.behandlingsflyt.integrasjon.unleash.UnleashService
import no.nav.aap.behandlingsflyt.integrasjon.utbetaling.UtbetalingGatewayImpl
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRegisterGatewayImpl
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.pip.behandlingsflytPip
import no.nav.aap.behandlingsflyt.prosessering.BehandlingsflytLogInfoProvider
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.vedtak.VedtakRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg.BarnetilleggRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningAndreStatligeYtelserRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.SamordningYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering.SamordningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid.MeldekortRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg.MedlemskapArbeidInntektRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningForutgåendeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.personopplysning.PersonopplysningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt.InntektGrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade.YrkesskadeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.bistand.BistandRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.rettighetsperiode.VurderRettighetsperiodeRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.student.StudentRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykdomRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.søknad.TrukketSøknadRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.log.ContextRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.saksApi
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.komponenter.server.plugins.NavIdentInterceptor
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.retry.RetryService
import org.slf4j.LoggerFactory
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
        connector {
            port = 8080
        }
    }) { server(DbConfig()) }.start(wait = true)
}

internal fun Application.server(dbConfig: DbConfig) {
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
    val motor = startMotor(dataSource)

    registerGateways()
    registerRepositories()

    if (!Miljø.erLokal()) {
        /* sjekk at feature toggles funker i dev og prod før det tas i bruk .. */
        LoggerFactory.getLogger("main").info(
            "test feature toggle: {}",
            GatewayProvider.provide<UnleashGateway>().isEnabled(BehandlingsflytFeature.FasttrackMeldekort)
        )
    }

    routing {
        authenticate(AZURE) {
            install(NavIdentInterceptor)

            apiRouting {
                configApi()
                saksApi(dataSource)
                behandlingApi(dataSource)
                flytApi(dataSource)
                fatteVedtakGrunnlagApi(dataSource)
                kvalitetssikringApi(dataSource)
                bistandsgrunnlagApi(dataSource)
                meldepliktsgrunnlagApi(dataSource)
                arbeidsevneGrunnlagApi(dataSource)
                medlemskapsgrunnlagApi(dataSource)
                studentgrunnlagApi(dataSource)
                sykdomsgrunnlagApi(dataSource)
                sykepengerGrunnlagApi(dataSource)
                institusjonAPI(dataSource)
                avklaringsbehovApi(dataSource)
                tilkjentYtelseAPI(dataSource)
                trukketSøknadGrunnlagAPI(dataSource)
                rettighetsperiodeGrunnlagAPI(dataSource)
                beregningVurderingAPI(dataSource)
                beregningsGrunnlagApi(dataSource)
                aldersGrunnlagApi(dataSource)
                barnetilleggApi(dataSource)
                motorApi(dataSource)
                behandlingsflytPip(dataSource)
                aktivitetspliktApi(dataSource)
                auditlogApi(dataSource)
                refusjonGrunnlagAPI(dataSource)
                // Flytt
                brevApi(dataSource)
                dokumentinnhentingAPI(dataSource)
                mottattHendelseApi(dataSource)
                underveisVurderingerAPI(dataSource)
                lovvalgMedlemskapAPI(dataSource)
                lovvalgMedlemskapGrunnlagAPI(dataSource)
                samordningGrunnlag(dataSource)
                forutgåendeMedlemskapAPI(dataSource)
                driftAPI(dataSource)
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
        .register<StatistikkGatewayImpl>()
        .register<InntektGatewayImpl>()
        .register<BrevGateway>()
        .register<OppgavestyringGatewayImpl>()
        .register<UføreGateway>()
        .register<YrkesskadeRegisterGatewayImpl>()
        .register<MeldekortGatewayImpl>()
        .register<TilgangGatewayImpl>()
        .register<TjenestePensjonGatewayImpl>()
        .register<UnleashService>()
        .status()
}

private fun registerRepositories() {
    RepositoryRegistry.register<BehandlingRepositoryImpl>()
        .register<PersonRepositoryImpl>()
        .register<SakRepositoryImpl>()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<VilkårsresultatRepositoryImpl>()
        .register<PipRepositoryImpl>()
        .register<TaSkriveLåsRepositoryImpl>()
        .register<BeregningsgrunnlagRepositoryImpl>()
        .register<PersonopplysningRepositoryImpl>()
        .register<TilkjentYtelseRepositoryImpl>()
        .register<AktivitetspliktRepositoryImpl>()
        .register<BrevbestillingRepositoryImpl>()
        .register<SamordningRepositoryImpl>()
        .register<MottattDokumentRepositoryImpl>()
        .register<MeldekortRepositoryImpl>()
        .register<UnderveisRepositoryImpl>()
        .register<ArbeidsevneRepositoryImpl>()
        .register<Effektuer11_7RepositoryImpl>()
        .register<BarnetilleggRepositoryImpl>()
        .register<ContextRepositoryImpl>()
        .register<BistandRepositoryImpl>()
        .register<BeregningVurderingRepositoryImpl>()
        .register<SykdomRepositoryImpl>()
        .register<YrkesskadeRepositoryImpl>()
        .register<UføreRepositoryImpl>()
        .register<MedlemskapArbeidInntektRepositoryImpl>()
        .register<SykepengerErstatningRepositoryImpl>()
        .register<SamordningVurderingRepositoryImpl>()
        .register<SamordningYtelseRepositoryImpl>()
        .register<SamordningUføreRepositoryImpl>()
        .register<SamordningAndreStatligeYtelserRepositoryImpl>()
        .register<StudentRepositoryImpl>()
        .register<MeldepliktRepositoryImpl>()
        .register<MedlemskapArbeidInntektForutgåendeRepositoryImpl>()
        .register<PersonopplysningForutgåendeRepositoryImpl>()
        .register<BarnRepositoryImpl>()
        .register<InstitusjonsoppholdRepositoryImpl>()
        .register<InntektGrunnlagRepositoryImpl>()
        .register<MeldeperiodeRepositoryImpl>()
        .register<VedtakRepositoryImpl>()
        .register<RefusjonkravRepositoryImpl>()
        .register<InformasjonskravRepositoryImpl>()
        .register<TjenestePensjonRepositoryImpl>()
        .register<TrukketSøknadRepositoryImpl>()
        .register<VurderRettighetsperiodeRepositoryImpl>()
        .register<FlytJobbRepositoryImpl>()
        .status()
}

fun Application.startMotor(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = ANTALL_WORKERS,
        logInfoProvider = BehandlingsflytLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = prometheus,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        motor.stop()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStarted) {}
        application.monitor.unsubscribe(ApplicationStopped) {}
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

fun initDatasource(dbConfig: DbConfig): DataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10 + (ANTALL_WORKERS * 2)
    minimumIdle = 1
    connectionTestQuery = "SELECT 1"
    metricRegistry = prometheus
})
