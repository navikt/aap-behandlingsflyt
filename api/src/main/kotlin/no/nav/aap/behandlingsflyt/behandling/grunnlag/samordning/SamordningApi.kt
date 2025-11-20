package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.samordning.EndringStatus
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningPeriodeSammenligner
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeMedEndringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UførePeriodeSammenligner
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource

/**
 * @param ytelser Hvilke ytelser det er funnet på denne personen.
 * @param vurdering Manuelle vurderinger gjort av saksbehandler for gitte ytelser.
 */
data class SamordningYtelseVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val ytelser: List<SamordningYtelseDTO>,
    val vurdering: SamordningYtelseVurderingDTO?,
    val historiskeVurderinger: List<SamordningYtelseVurderingDTO>,
    val tpYtelser: List<TjenestePensjonForhold>?,
)

data class SamordningYtelseVurderingDTO(
    val begrunnelse: String?,
    val fristNyRevurdering: LocalDate?,
    val maksDatoEndelig: Boolean?,
    val vurderinger: List<SamordningVurderingDTO>,
    val vurdertAv: VurdertAvResponse?
)

data class SamordningYtelseDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val kilde: String,
    val saksRef: String?,
    val endringStatus: EndringStatus
)

data class SamordningVurderingDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val manuell: Boolean?
)

data class SamordningUføreVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningUføreVurderingDTO?,
    val grunnlag: List<SamordningUføreGrunnlagDTO>
)

data class SamordningUføreGrunnlagDTO(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Int,
    val kilde: String,
    val endringStatus: EndringStatus
)

data class SamordningUføreVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDTO>,
    val vurdertAv: VurdertAvResponse
)

data class SamordningUføreVurderingPeriodeDTO(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int
)

data class SamordningAndreStatligeYtelserGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningAndreStatligeYtelserVurderingDTO?,
    val historiskeVurderinger: List<SamordningAndreStatligeYtelserVurderingDTO> = emptyList(),
)

data class SamordningAndreStatligeYtelserVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriodeDTO>,
    val vurdertAv: VurdertAvResponse?
)

data class SamordningAndreStatligeYtelserVurderingPeriodeDTO(
    val periode: Periode,
    val ytelse: AndreStatligeYtelser,
)

data class SamordningArbeidsgiverGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningArbeidsgiverVurderingDTO?
)

data class SamordningArbeidsgiverVurderingDTO(
    val begrunnelse: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vurdertAv: VurdertAvResponse?
)

data class TjenestepensjonGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val tjenestepensjonYtelser: List<TjenestepensjonYtelseDTO>,
    val tjenestepensjonRefusjonskravVurdering: TjenestepensjonRefusjonskravVurdering? = null
)

data class TjenestepensjonYtelseDTO(
    val ytelseIverksattFom: LocalDate,
    val ytelseIverksattTom: LocalDate?,
    val ytelse: YtelseTypeCode,
    val ordning: TjenestePensjonOrdning
)

fun NormalOpenAPIRoute.samordningGrunnlag(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/samordning-ufore") {
            getGrunnlag<BehandlingReferanse, SamordningUføreVurderingGrunnlagDTO>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                ),
                avklaringsbehovKode = Definisjon.AVKLAR_SAMORDNING_UFØRE.kode.toString()
            ) { behandlingReferanse ->
                val (registerGrunnlag, vurdering) =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val samordningUføreRepository = repositoryProvider.provide<SamordningUføreRepository>()
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                        val uføreRepository = repositoryProvider.provide<UføreRepository>()

                        val behandling = behandlingRepository.hent(behandlingReferanse)
                        val samordningUføreVurdering =
                            samordningUføreRepository.hentHvisEksisterer(behandling.id)?.vurdering
                        val uføregrunnlagMedEndretStatus =
                            UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandling.id)

                        Pair(uføregrunnlagMedEndretStatus, samordningUføreVurdering)
                    }


                respond(
                    SamordningUføreVurderingGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = mapSamordningUføreVurdering(vurdering, ansattInfoService),
                        grunnlag = mapSamordningUføreGrunnlag(registerGrunnlag)
                    )
                )
            }
        }
        route("/{referanse}/grunnlag/samordning/tjenestepensjon") {
            getGrunnlag<BehandlingReferanse, TjenestepensjonGrunnlagDTO>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam =
                    BehandlingPathParam(
                        "referanse"
                    ),
                avklaringsbehovKode = Definisjon.SAMORDNING_REFUSJONS_KRAV.kode.toString(),
            ) { req ->
                val (tp, vurdering) =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()
                        val tjenestepensjonRefusjonsKravVurderingRepository =
                            repositoryProvider.provide<TjenestepensjonRefusjonsKravVurderingRepository>()
                        val behandling =
                            BehandlingReferanseService(
                                repositoryProvider.provide<BehandlingRepository>()
                            ).behandling(req)
                        val tp = tjenestePensjonRepository.hent(behandling.id)
                        val vurdering =
                            tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(
                                behandling.id
                            )
                        Pair(tp, vurdering)
                    }

                respond(
                    TjenestepensjonGrunnlagDTO(
                        kanSaksbehandle(),
                        tp.flatMap { ordning ->
                            ordning.ytelser.map { ytelse ->
                                TjenestepensjonYtelseDTO(
                                    ytelseIverksattFom = ytelse.ytelseIverksattFom,
                                    ytelseIverksattTom = ytelse.ytelseIverksattTom,
                                    ytelse = ytelse.ytelseType,
                                    ordning = ordning.ordning
                                )
                            }
                        },
                        vurdering
                    )
                )
            }
        }

        route("/{referanse}/grunnlag/samordning") {
            getGrunnlag<BehandlingReferanse, SamordningYtelseVurderingGrunnlagDTO>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SAMORDNING_GRADERING.kode.toString()
            ) { req ->
                val (registerYtelser, samordningPair, tp) =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val samordningRepository = repositoryProvider.provide<SamordningVurderingRepository>()
                        val samordningYtelseRepository = repositoryProvider.provide<SamordningYtelseRepository>()
                        val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()

                        val behandling =
                            BehandlingReferanseService(
                                repositoryProvider.provide<BehandlingRepository>()
                            ).behandling(req)

                        val samordning = samordningRepository.hentHvisEksisterer(behandling.id)
                        val historiskeVurderinger =
                            samordningRepository.hentHistoriskeVurderinger(behandling.sakId, behandling.id)

                        val perioderMedEndringer =
                            SamordningPeriodeSammenligner(samordningYtelseRepository).hentPerioderMarkertMedEndringer(
                                behandling.id
                            )

                        val tp = tjenestePensjonRepository.hentHvisEksisterer(behandling.id)

                        Triple(perioderMedEndringer, Pair(samordning, historiskeVurderinger), tp)
                    }

                val (samordning, historiskeVurderinger) = samordningPair

                respond(
                    SamordningYtelseVurderingGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        ytelser =
                            registerYtelser.map { ytelse ->
                                SamordningYtelseDTO(
                                    ytelseType = ytelse.ytelseType,
                                    periode = Periode(fom = ytelse.periode.fom, tom = ytelse.periode.tom),
                                    gradering = ytelse.gradering?.prosentverdi(),
                                    kronesum = ytelse.kronesum?.toInt(),
                                    kilde = ytelse.kilde,
                                    saksRef = ytelse.saksRef,
                                    endringStatus = ytelse.endringStatus
                                )
                            },
                        vurdering = samordning?.let {
                            mapSamordningVurdering(it, ansattInfoService)
                        },
                        historiskeVurderinger = historiskeVurderinger.map {
                            mapSamordningVurdering(it, ansattInfoService)
                        },
                        tpYtelser = tp,
                    )
                )
            }
        }

        route("/{referanse}/grunnlag/samordning-andre-statlige-ytelser") {
            getGrunnlag<BehandlingReferanse, SamordningAndreStatligeYtelserGrunnlagDTO>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam =
                    BehandlingPathParam(
                        "referanse"
                    ),
                avklaringsbehovKode = Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER.kode.toString(),
            ) { behandlingReferanse ->
                val (samordningAndreStatligeYtelserVurdering, samordningAndreStatligeYtelserHistoriskeVurdering) =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val samordningAndreStatligeYtelserRepository =
                            repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>()

                        val sakRepository = repositoryProvider.provide<SakRepository>()
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                        val behandling = behandlingRepository.hent(behandlingReferanse)
                        val sak = sakRepository.hent(behandling.sakId)

                        val historiskeBehandlinger = behandlingRepository.hentAlleFor(
                            sak.id,
                            TypeBehandling.ytelseBehandlingstyper()
                        ).filter { it.id != behandling.id }

                        val vurdering =
                            samordningAndreStatligeYtelserRepository.hentHvisEksisterer(behandling.id)?.vurdering

                        val historiskeVurderinger =
                            historiskeBehandlinger.mapNotNull { historiskeBehandling ->
                                samordningAndreStatligeYtelserRepository.hentHvisEksisterer(historiskeBehandling.id)?.vurdering
                            }

                        Pair(vurdering, historiskeVurderinger)
                    }

                val navnOgEnhet = samordningAndreStatligeYtelserVurdering?.let {
                    ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)
                }

                val historiskeVurderinger = samordningAndreStatligeYtelserHistoriskeVurdering
                    .map { denneVurdering ->
                        SamordningAndreStatligeYtelserVurderingDTO(
                            begrunnelse = denneVurdering.begrunnelse,
                            denneVurdering.vurderingPerioder
                                .map {
                                    SamordningAndreStatligeYtelserVurderingPeriodeDTO(
                                        periode = it.periode,
                                        ytelse = it.ytelse,
                                    )
                                },
                            vurdertAv =
                                denneVurdering.let {
                                    VurdertAvResponse(
                                        ident = it.vurdertAv,
                                        dato =
                                            requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                                                "Fant ikke vurdert tidspunkt for samordningAndreStatligeYtelserVurdering"
                                            },
                                        ansattnavn = navnOgEnhet?.navn,
                                        enhetsnavn = navnOgEnhet?.enhet
                                    )
                                }
                        )

                    }

                val vurdering = samordningAndreStatligeYtelserVurdering?.let { vurdering ->
                    SamordningAndreStatligeYtelserVurderingDTO(
                        begrunnelse = vurdering.begrunnelse,
                        vurderingPerioder =
                            vurdering.vurderingPerioder
                                .map {
                                    SamordningAndreStatligeYtelserVurderingPeriodeDTO(
                                        periode = it.periode,
                                        ytelse = it.ytelse,
                                    )
                                },
                        vurdertAv =
                            vurdering.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato =
                                        requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                                            "Fant ikke vurdert tidspunkt for samordningAndreStatligeYtelserVurdering"
                                        },
                                    ansattnavn = navnOgEnhet?.navn,
                                    enhetsnavn = navnOgEnhet?.enhet
                                )
                            })
                }

                respond(
                    SamordningAndreStatligeYtelserGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = vurdering,
                        historiskeVurderinger = historiskeVurderinger,
                    )
                )
            }
        }


        route("/{referanse}/grunnlag/samordning-arbeidsgiver") {
            getGrunnlag<BehandlingReferanse, SamordningArbeidsgiverGrunnlagDTO>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam =
                    BehandlingPathParam(
                        "referanse"
                    ),
                avklaringsbehovKode = Definisjon.SAMORDNING_ARBEIDSGIVER.kode.toString(),
            ) { behandlingReferanse ->
                val samordningArbeidsgiverVurdering =
                    dataSource.transaction { connection ->
                        val repositoryProvider = repositoryRegistry.provider(connection)
                        val samordningArbeidsgiverRepository =
                            repositoryProvider.provide<SamordningArbeidsgiverRepository>()
                        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

                        val behandling = behandlingRepository.hent(behandlingReferanse)

                        samordningArbeidsgiverRepository.hentHvisEksisterer(behandling.id)?.vurdering
                    }

                val navnOgEnhet = samordningArbeidsgiverVurdering?.let {
                    ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)
                }

                val vurdering = samordningArbeidsgiverVurdering?.let { vurdering ->
                    SamordningArbeidsgiverVurderingDTO(
                        vurdertAv = VurdertAvResponse(
                            ident = vurdering.vurdertAv,
                            dato = requireNotNull(vurdering.vurdertTidspunkt?.toLocalDate()) {
                                "Fant ikke vurdert tidspunkt for samordningArbeidsgiverVurdering"
                            },
                            ansattnavn = navnOgEnhet?.navn,
                            enhetsnavn = navnOgEnhet?.enhet
                        ),
                        begrunnelse = vurdering.begrunnelse,
                        fom = vurdering.fom,
                        tom = vurdering.tom
                    )
                }

                respond(
                    SamordningArbeidsgiverGrunnlagDTO(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        vurdering = vurdering
                    )
                )
            }
        }
    }
}

private fun mapSamordningVurdering(
    samordning: SamordningVurderingGrunnlag,
    ansattInfoService: AnsattInfoService
): SamordningYtelseVurderingDTO {
    val ansattNavnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(samordning.vurdertAv)

    return SamordningYtelseVurderingDTO(
        begrunnelse = samordning.begrunnelse,
        fristNyRevurdering = samordning.fristNyRevurdering,
        maksDatoEndelig = samordning.maksDatoEndelig,
        vurderinger = samordning.vurderinger.flatMap { vurdering ->
            vurdering.vurderingPerioder.map {
                SamordningVurderingDTO(
                    ytelseType = vurdering.ytelseType,
                    gradering = it.gradering?.prosentverdi(),
                    periode = it.periode,
                    kronesum = it.kronesum?.toInt(),
                    manuell = it.manuell
                )
            }
        },
        vurdertAv = VurdertAvResponse(
            ident = samordning.vurdertAv,
            dato = requireNotNull(samordning.vurdertTidspunkt?.toLocalDate()) {
                "Fant ikke vurderingstidspunkt for yrkesskadevurdering"
            },
            ansattnavn = ansattNavnOgEnhet?.navn,
            enhetsnavn = ansattNavnOgEnhet?.enhet
        ),
    )
}

private fun mapSamordningUføreVurdering(
    vurdering: SamordningUføreVurdering?,
    ansattInfoService: AnsattInfoService,
): SamordningUføreVurderingDTO? =
    vurdering?.let {
        val navnOgEnhet = ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv)

        return SamordningUføreVurderingDTO(
            begrunnelse = it.begrunnelse,
            vurderingPerioder =
                it.vurderingPerioder.map { periode ->
                    SamordningUføreVurderingPeriodeDTO(
                        periode.virkningstidspunkt,
                        periode.uføregradTilSamordning.prosentverdi()
                    )
                },
            vurdertAv = VurdertAvResponse(
                ident = it.vurdertAv,
                dato = requireNotNull(it.vurdertTidspunkt?.toLocalDate()) {
                    "Fant ikke vurderingstidspunkt for samordning uføre"
                },
                ansattnavn = navnOgEnhet?.navn,
                enhetsnavn = navnOgEnhet?.enhet
            )
        )
    }

private fun mapSamordningUføreGrunnlag(
    registerGrunnlagVurderinger: List<UførePeriodeMedEndringStatus>
): List<SamordningUføreGrunnlagDTO> =
    registerGrunnlagVurderinger.map {
        SamordningUføreGrunnlagDTO(
            virkningstidspunkt = it.virkningstidspunkt,
            uføregrad = it.uføregrad.prosentverdi(),
            // Alltid PESYS
            kilde = "PESYS",
            endringStatus = it.endringStatus
        )
    }