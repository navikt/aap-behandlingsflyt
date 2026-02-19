package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource
import kotlin.collections.fold

fun NormalOpenAPIRoute.institusjonApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/soning") {
            getGrunnlag<BehandlingReferanse, SoningsGrunnlagDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_SONINGSFORRHOLD.kode.toString()
            ) { req ->
                val soningsgrunnlag = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)

                    val utlederService =
                        InstitusjonsoppholdUtlederService(
                            barnetilleggRepository, institusjonsoppholdRepository,
                            sakRepository,
                            behandlingRepository
                        )
                    val behov = utlederService.utled(behandling.id)

                    // Hent ut rå fakta fra grunnlaget
                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val soningsforholdInfo =
                        byggTidslinjeAvType(grunnlag, Institusjonstype.FO)

                    val perioderMedSoning = behov.perioderTilVurdering.mapValue { it.soning }.komprimer()
                    val vurderinger = grunnlag?.soningsVurderinger?.tilTidslinje() ?: Tidslinje()

                    val manglendePerioder =
                        perioderMedSoning
                            .segmenter()
                            .filterNot { it.verdi == null }
                            .map {
                                SoningsforholdDto(
                                    vurderingsdato = it.periode.fom,
                                    vurdering =
                                        vurderinger.segment(it.periode.fom)?.verdi?.let { vurdering ->
                                            SoningsvurderingDto(
                                                skalOpphøre = vurdering.skalOpphøre,
                                                begrunnelse = vurdering.begrunnelse,
                                                fraDato = vurdering.fraDato
                                            )
                                        },
                                    status = it.verdi!!.vurdering.toDto()
                                )
                            }

                    val ansattNavnOgEnhet =
                        grunnlag?.soningsVurderinger?.let { ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv) }


                    SoningsGrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        soningsforholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        manglendePerioder,
                        vurdertAv =
                            grunnlag?.soningsVurderinger?.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato = it.vurdertTidspunkt.toLocalDate(),
                                    ansattnavn = ansattNavnOgEnhet?.navn,
                                    enhetsnavn = ansattNavnOgEnhet?.enhet
                                )
                            }
                    )
                }
                respond(soningsgrunnlag)
            }
        }
    }
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helse") {
            getGrunnlag<BehandlingReferanse, HelseinstitusjonGrunnlagDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_HELSEINSTITUSJON.kode.toString()
            ) { req ->
                val grunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()

                    val utlederService =
                        InstitusjonsoppholdUtlederService(
                            barnetilleggRepository, institusjonsoppholdRepository,
                            sakRepository,
                            behandlingRepository
                        )
                    val behov = utlederService.utled(behandling.id)

                    // Hent ut rå fakta fra grunnlaget
                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val oppholdInfo = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)

                    val perioderMedHelseopphold = behov.perioderTilVurdering.mapValue { it.helse }.komprimer()
                    val vurderinger = grunnlag?.helseoppholdvurderinger?.tilTidslinje().orEmpty()

                    val manglendePerioder = perioderMedHelseopphold.segmenter()
                        .filterNot { it.verdi == null }
                        .map {
                            HelseoppholdDto(
                                periode = it.periode,
                                oppholdId = null,
                                vurderinger = vurderinger.begrensetTil(it.periode).segmenter()
                                    .map { helseinstitusjonsvurdering ->
                                        HelseinstitusjonVurderingDto(
                                            oppholdId = null,
                                            begrunnelse = helseinstitusjonsvurdering.verdi.begrunnelse,
                                            faarFriKostOgLosji = helseinstitusjonsvurdering.verdi.faarFriKostOgLosji,
                                            forsoergerEktefelle = helseinstitusjonsvurdering.verdi.forsoergerEktefelle,
                                            harFasteUtgifter = helseinstitusjonsvurdering.verdi.harFasteUtgifter,
                                            periode = helseinstitusjonsvurdering.periode,
                                            vurdertAv = null
                                        )
                                    },
                                status = it.verdi!!.vurdering.toDto()
                            )
                        }

                    val ansattNavnOgEnhet =
                        grunnlag?.helseoppholdvurderinger?.let {
                            ansattInfoService.hentAnsattNavnOgEnhet(
                                it.vurdertAv
                            )
                        }

                    HelseinstitusjonGrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        opphold = oppholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        vurderinger = manglendePerioder,
                        vurdertAv =
                            grunnlag?.helseoppholdvurderinger?.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato = it.vurdertTidspunkt.toLocalDate(),
                                    ansattnavn = ansattNavnOgEnhet?.navn,
                                    enhetsnavn = ansattNavnOgEnhet?.enhet
                                )
                            },
                        vedtatteVurderinger = emptyList()
                    )
                }
                respond(grunnlagDto)
            }
        }
    }
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helseny") {
            getGrunnlag<BehandlingReferanse, HelseinstitusjonGrunnlagDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                avklaringsbehovKode = Definisjon.AVKLAR_HELSEINSTITUSJON.kode.toString()
            ) { req ->
                val grunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()

                    val utlederService = InstitusjonsoppholdUtlederServiceNy(
                        barnetilleggRepository,
                        institusjonsoppholdRepository,
                        sakRepository,
                        behandlingRepository
                    )
                    val behov = utlederService.utled(behandling.id)

                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val oppholdInfo = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)

                    // Hent alle vurderinger gruppert per opphold fra repository
                    val vurderingerGruppertPerOpphold =
                        institusjonsoppholdRepository.hentVurderingerGruppertPerOpphold(behandling.id)

                    val nyeVurderingerForOpphold =
                        vurderingerGruppertPerOpphold.mapValues { (_, vurderinger) ->
                            vurderinger.filter { it.vurdertIBehandling == behandling.id }
                        }.filterValues { it.isNotEmpty() }

                    val vedtatteVurderingerForOpphold = behandling.forrigeBehandlingId?.let {
                        institusjonsoppholdRepository.hentVurderingerGruppertPerOpphold(it)
                    } ?: emptyMap()

                    val helseoppholdPerioder = behov.perioderTilVurdering.mapValue { it.helse }.komprimer()

                    val vedtatteVurderingerDto =
                        mapVurderingerToDto(vedtatteVurderingerForOpphold, oppholdInfo, ansattInfoService)

                    val vurderingerDto = if (nyeVurderingerForOpphold.isEmpty()) {
                        helseoppholdPerioder.segmenter()
                            .mapNotNull { segment ->
                                val verdi = segment.verdi
                                if (verdi != null && verdi.vurdering == OppholdVurdering.UAVKLART) {
                                    oppholdInfo.begrensetTil(segment.periode).segmenter().map { oppholdSegment ->
                                        HelseoppholdDto(
                                            periode = oppholdSegment.periode,
                                            oppholdId = lagOppholdId(
                                                oppholdSegment.verdi.navn,
                                                oppholdSegment.periode.fom
                                            ),
                                            vurderinger = emptyList(),
                                            status = OppholdVurderingDto.UAVKLART
                                        )
                                    }
                                } else null
                            }.flatten()
                    } else {
                        val uavklarteDto = helseoppholdPerioder.segmenter()
                            .filter { it.verdi != null && it.verdi?.vurdering == OppholdVurdering.UAVKLART }
                            .flatMap { segment ->
                                oppholdInfo.begrensetTil(segment.periode).segmenter().map { oppholdSegment ->
                                    HelseoppholdDto(
                                        periode = oppholdSegment.periode,
                                        oppholdId = lagOppholdId(
                                            oppholdSegment.verdi.navn,
                                            oppholdSegment.periode.fom
                                        ),
                                        vurderinger = emptyList(),
                                        status = OppholdVurderingDto.UAVKLART
                                    )
                                }
                            }

                        mapVurderingerToDto(nyeVurderingerForOpphold, oppholdInfo, ansattInfoService) + uavklarteDto
                    }

                    val ansattNavnOgEnhet =
                        grunnlag?.helseoppholdvurderinger?.let {
                            ansattInfoService.hentAnsattNavnOgEnhet(
                                it.vurdertAv
                            )
                        }

                    HelseinstitusjonGrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        opphold = oppholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        vurderinger = vurderingerDto,
                        vedtatteVurderinger = vedtatteVurderingerDto,
                        vurdertAv =
                            grunnlag?.helseoppholdvurderinger?.let {
                                VurdertAvResponse(
                                    ident = it.vurdertAv,
                                    dato = it.vurdertTidspunkt.toLocalDate(),
                                    ansattnavn = ansattNavnOgEnhet?.navn,
                                    enhetsnavn = ansattNavnOgEnhet?.enhet
                                )
                            }
                    )
                }
                respond(grunnlagDto)
            }
        }
    }
}

private fun mapVurderingerToDto(
    vurderingerPerOpphold: Map<Periode, List<HelseinstitusjonVurdering>>,
    oppholdInfo: Tidslinje<Institusjon>,
    ansattInfoService: AnsattInfoService
): List<HelseoppholdDto> =
    vurderingerPerOpphold.entries.flatMap { (vurderingPeriode, vurderingerForPeriode) ->
        oppholdInfo.begrensetTil(vurderingPeriode).segmenter().map { oppholdSegment ->
            HelseoppholdDto(
                periode = vurderingPeriode,
                oppholdId = lagOppholdId(oppholdSegment.verdi.navn, oppholdSegment.periode.fom),
                vurderinger = vurderingerForPeriode.map { vurdering ->
                    val navnOgEnhet = vurdering.let {
                        ansattInfoService.hentAnsattNavnOgEnhet(it.vurdertAv ?: "ukjent")
                    }

                    HelseinstitusjonVurderingDto(
                        oppholdId = lagOppholdId(
                            oppholdSegment.verdi.navn,
                            oppholdSegment.periode.fom
                        ),
                        begrunnelse = vurdering.begrunnelse,
                        faarFriKostOgLosji = vurdering.faarFriKostOgLosji,
                        forsoergerEktefelle = vurdering.forsoergerEktefelle,
                        harFasteUtgifter = vurdering.harFasteUtgifter,
                        periode = vurdering.periode,
                        vurdertAv = VurdertAvResponse(
                            ident = vurdering.vurdertAv ?: "ukjent",
                            dato = vurdering.vurdertTidspunkt?.toLocalDate() ?: LocalDate.now(),
                            ansattnavn = navnOgEnhet?.navn,
                            enhetsnavn = navnOgEnhet?.enhet
                        )
                    )
                },
                status = OppholdVurderingDto.UAVKLART
            )
        }
    }

// Public for testing
fun byggTidslinjeForInstitusjonsopphold(
    grunnlag: InstitusjonsoppholdGrunnlag?,
    type: Institusjonstype
): Tidslinje<Institusjon> {
    val segments = grunnlag
        ?.oppholdene
        ?.opphold
        ?.filter { it.verdi.type == type }
        ?.sortedBy { it.periode.fom }
        .orEmpty()

    if (segments.size < 2) return Tidslinje(segments)

    return segments
        .map { Tidslinje(it.periode, it.verdi) }
        .fold(Tidslinje<Institusjon>()) { eksisterende, tidslinje ->
            eksisterende.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }.komprimer()
}

private fun byggTidslinjeAvType(
    soningsopphold: InstitusjonsoppholdGrunnlag?, institusjonstype: Institusjonstype
): Tidslinje<Institusjon> {
    return Tidslinje(soningsopphold?.oppholdene?.opphold?.filter { it.verdi.type == institusjonstype }.orEmpty())
}
