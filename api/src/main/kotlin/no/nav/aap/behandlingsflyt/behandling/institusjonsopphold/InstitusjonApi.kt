package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.tilDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvService
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
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import java.time.LocalDate
import javax.sql.DataSource

fun NormalOpenAPIRoute.institusjonApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/soning") {
            getGrunnlag<BehandlingReferanse, SoningsGrunnlagDto>(
                relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                behandlingPathParam = BehandlingPathParam("referanse"),
                påkrevdRolle = Definisjon.AVKLAR_SONINGSFORRHOLD.løsesAv
            ) { req ->
                val soningsgrunnlag = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
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

                    SoningsGrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        soningsforholdInfo.segmenter().map { InstitusjonsoppholdDto.institusjonToDto(it) },
                        manglendePerioder,
                        vurderingerMeta =
                            grunnlag?.soningsVurderinger?.let {
                                vurdertAvService.byggVurderingerMeta(
                                    definisjon = Definisjon.AVKLAR_SONINGSFORRHOLD,
                                    behandlingId = behandling.id,
                                    vurdertAv = vurdertAvService.medNavnOgEnhet(
                                        ident = it.vurdertAv,
                                        tidspunkt = it.vurdertTidspunkt,
                                    ),
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
                påkrevdRolle = Definisjon.AVKLAR_HELSEINSTITUSJON.løsesAv
            ) { req ->
                val grunnlagDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val vurdertAvService = VurdertAvService(repositoryProvider, gatewayProvider)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val sakRepository = repositoryProvider.provide<SakRepository>()
                    val behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    val institusjonsoppholdRepository = repositoryProvider.provide<InstitusjonsoppholdRepository>()
                    val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()

                    val utlederService = InstitusjonsoppholdUtlederService(
                        barnetilleggRepository,
                        institusjonsoppholdRepository,
                        sakRepository,
                        behandlingRepository
                    )
                    val behov = utlederService.utled(behandling.id)

                    val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandling.id)
                    val oppholdInfo = byggTidslinjeForInstitusjonsopphold(grunnlag, Institusjonstype.HS)
                        .getOrThrow { UgyldigForespørselException(it.errorMessage) }

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
                        mapVurderingerToDto(
                            vedtatteVurderingerForOpphold,
                            oppholdInfo,
                            vurdertAvService
                        )

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

                        mapVurderingerToDto(
                            nyeVurderingerForOpphold,
                            oppholdInfo,
                            vurdertAvService
                        ) + uavklarteDto
                    }

                    val oppholdSegmenter = grunnlag?.oppholdene?.opphold
                        ?.filter { it.verdi.type == Institusjonstype.HS }
                        ?: emptyList()

                    // Beregn tidligste reduksjonsdato per opphold
                    val tidligsteReduksjonsdatoPerOpphold = beregnTidligsteReduksjonsdatoPerOpphold(oppholdSegmenter)

                    // Bygg opphold-liste med tidligsteReduksjonsdato
                    val oppholdMedReduksjonsdato = hentOppholdSomSkalVurderes(
                        oppholdInfo,
                        behov.perioderTilVurdering,
                        vedtatteVurderingerDto
                    ).map { dto ->
                        val matchendeSegment = oppholdSegmenter.find { segment ->
                            lagOppholdId(segment.verdi.navn, segment.periode.fom) == dto.oppholdId
                        }
                        dto.copy(
                            tidligsteReduksjonsdato = matchendeSegment?.let {
                                tidligsteReduksjonsdatoPerOpphold[it]
                            }
                        )
                    }

                    HelseinstitusjonGrunnlagDto(
                        harTilgangTilÅSaksbehandle = kanSaksbehandle(),
                        opphold = oppholdMedReduksjonsdato,
                        vurderinger = vurderingerDto,
                        vedtatteVurderinger = vedtatteVurderingerDto,
                    )
                }
                respond(grunnlagDto)
            }
        }
    }
}

fun mapVurderingerToDto(
    vurderingerPerOpphold: Map<Periode, List<HelseinstitusjonVurdering>>,
    oppholdInfo: Tidslinje<Institusjon>,
    vurdertAvService: VurdertAvService,
): List<HelseoppholdDto> {
    val alleKjeder = grupperSammenhengendeOppholdSegmenter(oppholdInfo.segmenter().toList())

    return vurderingerPerOpphold.entries.flatMap { (vurderingPeriode, vurderingerForPeriode) ->
        val kjede = alleKjeder.firstOrNull { it.periode.overlapper(vurderingPeriode) }
            ?: return@flatMap emptyList()
        val først = kjede.elementer.first()

        listOf(
            HelseoppholdDto(
                periode = vurderingPeriode,
                oppholdId = lagOppholdId(først.verdi.navn, først.periode.fom),
                delperioder = kjede.elementer.map {
                    InstitusjonsoppholdDelperiodeDto(it.verdi.navn, it.periode.fom, it.periode.tom)
                },
                vurderinger = vurderingerForPeriode.map { vurdering ->
                    HelseinstitusjonVurderingDto(
                        oppholdId = lagOppholdId(først.verdi.navn, først.periode.fom),
                        begrunnelse = vurdering.begrunnelse,
                        faarFriKostOgLosji = vurdering.faarFriKostOgLosji,
                        forsoergerEktefelle = vurdering.forsoergerEktefelle,
                        harFasteUtgifter = vurdering.harFasteUtgifter,
                        periode = vurdering.periode,
                        vurderingerMeta = vurdertAvService.byggVurderingerMeta(
                            definisjon = Definisjon.AVKLAR_HELSEINSTITUSJON,
                            behandlingId = vurdering.vurdertIBehandling,
                            vurdertAv = vurdertAvService.medNavnOgEnhet(
                                ident = vurdering.vurdertAv ?: Bruker("ukjent") /* hacky, burdeikke kalle PDL med ukjent som ident */,
                                dato = vurdering.vurdertTidspunkt?.toLocalDate() ?: LocalDate.now(),
                            ),
                        )
                    )
                },
                status = OppholdVurderingDto.UAVKLART
            )
        )
    }
}

// Public for testing
fun hentOppholdSomSkalVurderes(
    oppholdInfo: Tidslinje<Institusjon>,
    behovPerioder: Tidslinje<InstitusjonsoppholdVurdering>,
    vedtatteVurderingerDto: List<HelseoppholdDto>
): List<InstitusjonsoppholdDto> {
    val alleKjeder = grupperSammenhengendeOppholdSegmenter(oppholdInfo.segmenter().toList())

    val behovOpphold = alleKjeder
        .filter { kjede ->
            behovPerioder.segmenter().any { behovSegment ->
                kjede.periode.overlapper(behovSegment.periode)
            }
        }
        .map { it.tilDto() }

    val vedtatteOpphold = vedtatteVurderingerDto
        .mapNotNull { it.oppholdId }
        .distinct()
        .mapNotNull { oppholdId ->
            alleKjeder.firstOrNull { kjede ->
                kjede.elementer.any { segment ->
                    lagOppholdId(segment.verdi.navn, segment.periode.fom) == oppholdId
                }
            }?.tilDto()
        }

    return (behovOpphold + vedtatteOpphold).distinctBy { it.oppholdId }
}

private fun SammenhengendeOppholdGruppe.tilDto(): InstitusjonsoppholdDto {
    val først = elementer.first()
    val sist = elementer.last()
    return InstitusjonsoppholdDto(
        oppholdId = lagOppholdId(først.verdi.navn, først.periode.fom),
        institusjonstype = først.verdi.type.beskrivelse,
        oppholdstype = først.verdi.kategori.beskrivelse,
        status = if (sist.periode.tom > LocalDate.now()) StatusDto.AKTIV.toString() else StatusDto.AVSLUTTET.toString(),
        kildeinstitusjon = if (elementer.size == 1) først.verdi.navn
        else elementer.joinToString(" → ") { it.verdi.navn },
        oppholdFra = først.periode.fom,
        avsluttetDato = sist.periode.tom,
        tidligsteReduksjonsdato = null,
        delperioder = elementer.map {
            InstitusjonsoppholdDelperiodeDto(it.verdi.navn, it.periode.fom, it.periode.tom)
        }
    )
}

// Public for testing
fun byggTidslinjeForInstitusjonsopphold(
    grunnlag: InstitusjonsoppholdGrunnlag?,
    type: Institusjonstype
): Validation<Tidslinje<Institusjon>> {
    val segments = grunnlag
        ?.oppholdene
        ?.opphold
        ?.filter { it.verdi.type == type }
        ?.sortedBy { it.periode.fom }
        .orEmpty()

    if (segments.size < 2) return Validation.Valid(Tidslinje(segments))

    segments.zipWithNext { current, next ->
        if (current.periode.tom > next.periode.fom) {
            return Validation.Invalid(
                Tidslinje(),
                "Overlappende institusjonsopphold funnet: " +
                        "(${current.periode}) overlapper med " +
                        "(${next.periode}). " +
                        "Oppholdene må korrigeres i kildesystemet (INST2)."
            )
        }
    }

    val håndterOverlapp = segments.zipWithNext { current, next ->
        if (current.periode.tom == next.periode.fom) {
            current.copy(periode = Periode(current.periode.fom, current.periode.tom.minusDays(1)))
        } else {
            current
        }
    } + segments.last()

    return Validation.Valid(Tidslinje(håndterOverlapp))
}

private fun byggTidslinjeAvType(
    soningsopphold: InstitusjonsoppholdGrunnlag?, institusjonstype: Institusjonstype
): Tidslinje<Institusjon> {
    return Tidslinje(soningsopphold?.oppholdene?.opphold?.filter { it.verdi.type == institusjonstype }.orEmpty())
}
