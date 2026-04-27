package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakerRequest
import no.nav.aap.api.intern.behandlingsflyt.OppdaterIdenterDto
import no.nav.aap.api.intern.behandlingsflyt.SakStatusKelvin
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.MeldekortPerioderDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenaVedtaksvariantDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArenavedtakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.AvslagsårsakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.GjeldendeStansEllerOpphørDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.StansEllerOpphørEnumDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.prosessering.datadeling.UtledArenaVedtakstype
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate

class ApiInternGatewayImpl : ApiInternGateway {

    companion object : Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
            return ApiInternGatewayImpl()
        }

        private val arenaStatusCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(2))
            .maximumSize(10_000)
            .recordStats()
            .build<Set<String>, ArenaStatusResponse>()

        init {
            CaffeineCacheMetrics.monitor(prometheus, arenaStatusCache, "datadeling_arena_status")
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.datadeling.scope")),
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.datadeling.url"))

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        restClient.post(
            uri = uri.resolve("/api/insert/meldeperioder"),
            request = PostRequest(body = MeldekortPerioderDTO(ident, perioder)),
            mapper = { _, _ ->
            })
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        restClient.post(
            uri = uri.resolve("/api/insert/sakStatus"),
            request = PostRequest(
                body = SakStatusKelvin(
                    ident = ident, status = no.nav.aap.api.intern.behandlingsflyt.SakStatus(
                        sakId = sakStatus.sakId,
                        statusKode = when (sakStatus.status) {
                            SakStatus.DatadelingBehandlingStatus.SOKNAD_UNDER_BEHANDLING -> SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING
                            SakStatus.DatadelingBehandlingStatus.REVURDERING_UNDER_BEHANDLING -> SakstatusFraKelvin.REVURDERING_UNDER_BEHANDLING
                            SakStatus.DatadelingBehandlingStatus.FERDIGBEHANDLET -> SakstatusFraKelvin.FERDIGBEHANDLET
                        },
                        periode = no.nav.aap.api.intern.behandlingsflyt.Periode(
                            fom = sakStatus.periode.fom,
                            tom = sakStatus.periode.tom
                        )
                    )
                )
            ),
            mapper = { _, _ ->
            })
    }

    override fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal?,
        underveis: List<Underveisperiode>,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>,
        stansOpphørGrunnlag: Set<GjeldendeStansEllerOpphør>?,
        arenavedtak: Tidslinje<UtledArenaVedtakstype.ArenaVedtak>
    ) {
        log.info("Sender behandling for behandlingId=${behandling.id} med vedtakId=$vedtakId, sak: ${sak.saksnummer}. Beregningsgrunnlag: $beregningsgrunnlag")
        restClient.post(
            uri = uri.resolve("/api/insert/vedtak"),
            request = PostRequest(
                body = DatadelingDTO(
                    behandlingsId = behandling.id.id.toString(),
                    behandlingsReferanse = behandling.referanse.toString(),
                    underveisperiode = underveis.map {
                        UnderveisDTO(
                            underveisFom = it.periode.fom,
                            underveisTom = it.periode.tom,
                            meldeperiodeFom = it.meldePeriode.fom,
                            meldeperiodeTom = it.meldePeriode.tom,
                            utfall = it.utfall.name,
                            rettighetsType = it.rettighetsType?.name,
                            avslagsårsak = it.avslagsårsak?.name
                        )
                    },
                    rettighetsPeriodeFom = sak.rettighetsperiode.fom,
                    rettighetsPeriodeTom = sak.rettighetsperiode.tom,
                    behandlingStatus = behandling.status(),
                    vedtaksDato = vedtaksDato,
                    beregningsgrunnlag = beregningsgrunnlag,
                    sak = SakDTO(
                        saksnummer = sak.saksnummer.toString(),
                        status = sak.status(),
                        fnr = sak.person.identer().map { ident -> ident.identifikator },
                        opprettetTidspunkt = sak.opprettetTidspunkt
                    ),
                    tilkjent = tilkjent.map { tilkjentPeriode ->
                        TilkjentDTO(
                            tilkjentFom = tilkjentPeriode.periode.fom,
                            tilkjentTom = tilkjentPeriode.periode.tom,
                            dagsats = tilkjentPeriode.tilkjent.dagsats.verdi.toInt(),
                            // legg til redusert dagsats
                            gradering = tilkjentPeriode.tilkjent.gradering.prosentverdi(),
                            samordningUføregradering = tilkjentPeriode.tilkjent.graderingGrunnlag.samordningUføregradering.prosentverdi(),
                            // TODO: fjern
                            grunnlag = tilkjentPeriode.tilkjent.dagsats.verdi,
                            grunnlagsfaktor = tilkjentPeriode.tilkjent.grunnlagsfaktor.verdi(),
                            grunnbeløp = tilkjentPeriode.tilkjent.grunnbeløp.verdi,
                            antallBarn = tilkjentPeriode.tilkjent.antallBarn,
                            barnetilleggsats = tilkjentPeriode.tilkjent.barnetilleggsats.verdi,
                            barnetillegg = tilkjentPeriode.tilkjent.barnetillegg.verdi,
                        )
                    },
                    rettighetsTypeTidsLinje = rettighetsTypeTidslinje.segmenter().map { segment ->
                        RettighetsTypePeriode(
                            segment.periode.fom,
                            segment.periode.tom,
                            segment.verdi.toString()
                        )
                    },
                    vedtakId = vedtakId,
                    samId = samId,
                    stansOpphørVurdering = stansOpphørGrunnlag.orEmpty().map {
                        GjeldendeStansEllerOpphørDTO(
                            fom = it.fom,
                            opprettet = it.opprettet,
                            vurdering = when (it.vurdering) {
                                is Stans -> StansEllerOpphørEnumDTO.STANS
                                is Opphør -> StansEllerOpphørEnumDTO.OPPHØR
                            },
                            avslagsårsaker = it.vurdering.årsaker.mapNotNull { mapAvslagsårsak(it) }.toSet(),
                        )
                    }.toSet(),
                    arenavedtak = arenavedtak.segmenter().map {
                        ArenavedtakDTO(
                            vedtakId = it.verdi.vedtakId.id,
                            fom = it.fom(),
                            tom = it.tom(),
                            vedtaksvariant = when (it.verdi.vedtaksvariant) {
                                UtledArenaVedtakstype.ArenaVedtaksvariant.O_AVSLAG -> ArenaVedtaksvariantDTO.O_AVSLAG
                                UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_NAV -> ArenaVedtaksvariantDTO.O_INNV_NAV
                                UtledArenaVedtakstype.ArenaVedtaksvariant.O_INNV_SOKNAD -> ArenaVedtaksvariantDTO.O_INNV_SOKNAD
                                UtledArenaVedtakstype.ArenaVedtaksvariant.E_FORLENGE -> ArenaVedtaksvariantDTO.E_FORLENGE
                                UtledArenaVedtakstype.ArenaVedtaksvariant.E_VERDI -> ArenaVedtaksvariantDTO.E_VERDI
                                UtledArenaVedtakstype.ArenaVedtaksvariant.G_AVSLAG -> ArenaVedtaksvariantDTO.G_AVSLAG
                                UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_NAV -> ArenaVedtaksvariantDTO.G_INNV_NAV
                                UtledArenaVedtakstype.ArenaVedtaksvariant.G_INNV_SOKNAD -> ArenaVedtaksvariantDTO.G_INNV_SOKNAD
                                UtledArenaVedtakstype.ArenaVedtaksvariant.S_DOD -> ArenaVedtaksvariantDTO.S_DOD
                                UtledArenaVedtakstype.ArenaVedtaksvariant.S_OPPHOR -> ArenaVedtaksvariantDTO.S_OPPHOR
                                UtledArenaVedtakstype.ArenaVedtaksvariant.S_STANS -> ArenaVedtaksvariantDTO.S_STANS
                            }
                        )
                    },
                ),
            ),
            mapper = { _, _ ->
            })
    }

    fun mapAvslagsårsak(it: Avslagsårsak): AvslagsårsakDTO? {
        return when (it) {
            Avslagsårsak.BRUKER_OVER_67 -> AvslagsårsakDTO.BRUKER_OVER_67
            Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING -> AvslagsårsakDTO.IKKE_RETT_PA_SYKEPENGEERSTATNING
            Avslagsårsak.IKKE_RETT_PA_STUDENT -> AvslagsårsakDTO.IKKE_RETT_PA_STUDENT
            Avslagsårsak.VARIGHET_OVERSKREDET_STUDENT -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_STUDENT
            Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET -> AvslagsårsakDTO.IKKE_SYKDOM_AV_VISS_VARIGHET
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL -> AvslagsårsakDTO.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL
            Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE -> AvslagsårsakDTO.IKKE_NOK_REDUSERT_ARBEIDSEVNE
            Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING -> AvslagsårsakDTO.IKKE_BEHOV_FOR_OPPFOLGING
            Avslagsårsak.IKKE_MEDLEM -> AvslagsårsakDTO.IKKE_MEDLEM
            Avslagsårsak.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS -> AvslagsårsakDTO.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS
            Avslagsårsak.ANNEN_FULL_YTELSE -> AvslagsårsakDTO.ANNEN_FULL_YTELSE
            Avslagsårsak.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING -> AvslagsårsakDTO.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING
            Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE -> AvslagsårsakDTO.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_OVERGANG_UFORE
            Avslagsårsak.VARIGHET_OVERSKREDET_ARBEIDSSØKER -> AvslagsårsakDTO.VARIGHET_OVERSKREDET_ARBEIDSSØKER
            Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER -> AvslagsårsakDTO.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER
            Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING -> AvslagsårsakDTO.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS -> AvslagsårsakDTO.BRUDD_PÅ_AKTIVITETSPLIKT_STANS
            Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR -> AvslagsårsakDTO.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS -> AvslagsårsakDTO.BRUDD_PÅ_OPPHOLDSKRAV_STANS
            Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR -> AvslagsårsakDTO.BRUDD_PÅ_OPPHOLDSKRAV_OPPHØR
            Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP -> AvslagsårsakDTO.ORDINÆRKVOTE_BRUKT_OPP
            Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP -> AvslagsårsakDTO.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
            Avslagsårsak.BRUKER_UNDER_18 -> null
            Avslagsårsak.MANGLENDE_DOKUMENTASJON -> null
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE -> null
            Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT -> null
            Avslagsårsak.HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON -> null
        }
    }

    override fun sendDetaljertMeldekortListe(
        detaljertMeldekortListe: List<DetaljertMeldekortDTO>,
        sakId: SakId,
        behandlingId: BehandlingId
    ) {
        log.info("Sender meldekort-detaljer for sakId=${sakId}, behandlingId=${behandlingId}")

        restClient.post(
            uri.resolve("/api/insert/meldekort-detaljer"),
            PostRequest(body = detaljertMeldekortListe),
            mapper = { _, _ -> }
        )
    }

    override fun hentArenaStatus(personidentifikatorer: Set<String>): ArenaStatusResponse {
        // Kalles ofte fra saksbehandling, så cache den
        return arenaStatusCache.get(personidentifikatorer) {
            val sakerRequest = SakerRequest(personidentifikatorer = personidentifikatorer.toList())
            doHentArenaStatus(sakerRequest)
        }
    }

    override fun oppdaterIdenter(
        saksnummer: Saksnummer,
        identer: List<Ident>
    ) {
        log.info("Oppdaterer identer for sak $saksnummer.")
        restClient.post(
            uri.resolve("/api/insert/oppdater-identer"),
            PostRequest(body = OppdaterIdenterDto(saksnummer.toString(), identer.map(Ident::identifikator))),
            mapper = { _, _ -> }
        )
    }

    private fun doHentArenaStatus(sakerRequest: SakerRequest): ArenaStatusResponse {
        val remoteResponse: PersonEksistererIAAPArena? = restClient.post(
            uri.resolve("/arena/person/aap/eksisterer"),
            PostRequest(body = sakerRequest),
            mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }
        )
        requireNotNull(remoteResponse) { "Fikk ikke gyldig svar på om personen eksisterer i AAP-Arena" }
        return ArenaStatusResponse(remoteResponse.eksisterer)
    }
}
