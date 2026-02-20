package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.windowed

/** Når kan vi definitivt si at det er avslag, slik
 * at vi ikke trenger å vurdere flere vilkår.
 *
 * Det er viktig at vi kun ser fram til aktivt steg,
 * fordi selv om det er avslag på et steg senere i flyten, så kan det være
 * at den vurderingen endres slik at det ikke lenger er et avslag.
 */
interface TidligereVurderinger {
    fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun harBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return !girIngenBehandlingsgrunnlag(kontekst, førSteg)
    }

    fun muligMedRettTilAAP(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return !girAvslagEllerIngenBehandlingsgrunnlag(kontekst, førSteg)
    }

    sealed interface Behandlingsutfall
    data object IkkeBehandlingsgrunnlag : Behandlingsutfall
    data object UunngåeligAvslag : Behandlingsutfall
    data class PotensieltOppfylt(val rettighetstype: RettighetsType?) : Behandlingsutfall

    fun behandlingsutfall(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Tidslinje<Behandlingsutfall>
}

class TidligereVurderingerImpl(
    private val trukketSøknadService: TrukketSøknadService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val unleashGateway: UnleashGateway
) : TidligereVurderinger {

    private val log = LoggerFactory.getLogger(javaClass)

    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ) : this(
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    data class Sjekk(
        val steg: StegType,
        val sjekk: (vilkårsresultat: Vilkårsresultat, kontekst: FlytKontekstMedPerioder, tidligereVurderinger: Tidslinje<TidligereVurderinger.Behandlingsutfall>) -> Tidslinje<TidligereVurderinger.Behandlingsutfall>
    )

    private val sjekker = lagSjekker(
        listOf(
            Sjekk(StegType.AVBRYT_REVURDERING) { _, kontekst, _ ->
                Tidslinje(
                    kontekst.rettighetsperiode,
                    if (avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId))
                        TidligereVurderinger.IkkeBehandlingsgrunnlag
                    else
                        TidligereVurderinger.PotensieltOppfylt(null)
                )
            },

            Sjekk(StegType.SØKNAD) { _, kontekst, accBehandlingsutfall ->
                Tidslinje(
                    kontekst.rettighetsperiode,
                    if (trukketSøknadService.søknadErTrukket(kontekst.behandlingId))
                        TidligereVurderinger.IkkeBehandlingsgrunnlag
                    else
                        TidligereVurderinger.PotensieltOppfylt(null)
                )
            },

            Sjekk(StegType.VURDER_LOVVALG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.LOVVALG, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_ALDER) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.ALDERSVILKÅRET, vilkårsresultat)
            },

            Sjekk(StegType.AVKLAR_STUDENT) { vilkårsresultat, _, _ ->
                vilkårsresultat.tidslinjeFor(Vilkårtype.STUDENT).map {
                    TidligereVurderinger.PotensieltOppfylt(
                        when {
                            it.utfall == Utfall.OPPFYLT -> RettighetsType.STUDENT
                            else -> null
                        }
                    )
                }
            },

            Sjekk(StegType.AVKLAR_SYKDOM) { _, kontekst, tidligereVurderinger ->
                val sykdomstidslinje = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.somSykdomsvurderingstidslinje().orEmpty()

                tidligereVurderinger.leftJoin(sykdomstidslinje) { segmentPeriode, foreløpigUtfall, sykdomsvurdering ->
                    val sykdomDefinitivtAvslag =
                        sykdomsvurdering?.erOppfyltOrdinærSettBortIfraVissVarighet() == false
                                && !sykdomsvurdering.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet()

                    val foreløpigRettighetstype = when (foreløpigUtfall) {
                        is TidligereVurderinger.PotensieltOppfylt -> foreløpigUtfall.rettighetstype
                        else -> null
                    }

                    if (foreløpigRettighetstype == null && sykdomDefinitivtAvslag && !potensieltOppfyltOvergangArbeid(
                            kontekst.rettighetsperiode,
                            segmentPeriode,
                            sykdomstidslinje
                        )
                    ) {
                        return@leftJoin TidligereVurderinger.UunngåeligAvslag

                    }

                    return@leftJoin TidligereVurderinger.PotensieltOppfylt(null)
                }
            },

            Sjekk(StegType.VURDER_BISTANDSBEHOV) { _, kontekst, _ ->
                val bistandTidslinje =
                    bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje()
                        .orEmpty()
                val sykdomstidslinje = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.somSykdomsvurderingstidslinje().orEmpty()

                Tidslinje.map2(
                    sykdomstidslinje,
                    bistandTidslinje
                ) { segmentPeriode, sykdomvurdering, bistandvurdering ->
                    val erBistandOppfylt = bistandvurdering?.erBehovForBistand() == true
                    val erSykdomOppfyltOrdinærEllerPotensieltYrkesskade =
                        sykdomvurdering?.erOppfyltOrdinærEllerYrkesskadeSettBortIfraÅrsakssammenheng(
                            kontekst.rettighetsperiode.fom,
                            segmentPeriode
                        ) == true

                    when {
                        erBistandOppfylt && erSykdomOppfyltOrdinærEllerPotensieltYrkesskade -> TidligereVurderinger.PotensieltOppfylt(
                            RettighetsType.BISTANDSBEHOV
                        )

                        else -> TidligereVurderinger.PotensieltOppfylt(null)
                    }
                }
            },

            Sjekk(StegType.OVERGANG_UFORE) { vilkårsresultat, _, _ ->
                vilkårsresultat.tidslinjeFor(Vilkårtype.OVERGANGUFØREVILKÅRET).map {
                    TidligereVurderinger.PotensieltOppfylt(
                        when {
                            it.utfall == Utfall.OPPFYLT -> RettighetsType.VURDERES_FOR_UFØRETRYGD
                            else -> null
                        }
                    )
                }
            },

            Sjekk(StegType.OVERGANG_ARBEID) { vilkårsresultat, _, _ ->
                vilkårsresultat.tidslinjeFor(Vilkårtype.OVERGANGARBEIDVILKÅRET).map {
                    TidligereVurderinger.PotensieltOppfylt(
                        when {
                            it.utfall == Utfall.OPPFYLT -> RettighetsType.ARBEIDSSØKER
                            else -> null
                        }
                    )
                }
            },

            Sjekk(StegType.VURDER_SYKEPENGEERSTATNING) { vilkårsresultat, _, tidligereVurderinger ->
                vilkårsresultat.tidslinjeFor(Vilkårtype.SYKEPENGEERSTATNING)
                    .leftJoin(tidligereVurderinger) { speVilkår, akkumulertUtfall ->
                        when {
                            speVilkår.utfall == Utfall.OPPFYLT -> TidligereVurderinger.PotensieltOppfylt(
                                RettighetsType.SYKEPENGEERSTATNING
                            )
                            // Siste mulige rettighetstype
                            akkumulertUtfall is TidligereVurderinger.PotensieltOppfylt && akkumulertUtfall.rettighetstype == null -> TidligereVurderinger.UunngåeligAvslag

                            else -> TidligereVurderinger.PotensieltOppfylt(null)
                        }
                    }
            },

            Sjekk(StegType.FASTSETT_SYKDOMSVILKÅRET) { vilkårsresultat, _, tidligereVurderinger ->
                tidligereVurderinger.leftJoin(vilkårsresultat.tidslinjeFor(Vilkårtype.SYKDOMSVILKÅRET)) { akkumulertUtfall, sykdomsvilkåret ->
                    when {
                        akkumulertUtfall is TidligereVurderinger.PotensieltOppfylt && akkumulertUtfall.rettighetstype == RettighetsType.BISTANDSBEHOV && sykdomsvilkåret?.utfall == IKKE_OPPFYLT -> TidligereVurderinger.UunngåeligAvslag
                        else -> TidligereVurderinger.PotensieltOppfylt(null)
                    }
                }
            },

            Sjekk(StegType.FASTSETT_GRUNNLAG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.GRUNNLAGET, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_INNTEKTSBORTFALL) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.INNTEKTSBORTFALL, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_MEDLEMSKAP) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.MEDLEMSKAP, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_AVSLAG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_SYKESTIPEND) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING, vilkårsresultat)
            },
        )
    )

    private val gamleSjekker = lagSjekker(
        listOf(
            Sjekk(StegType.AVBRYT_REVURDERING) { _, kontekst, _ ->
                Tidslinje(
                    kontekst.rettighetsperiode,
                    if (avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId))
                        TidligereVurderinger.IkkeBehandlingsgrunnlag
                    else
                        TidligereVurderinger.PotensieltOppfylt(null)
                )
            },

            Sjekk(StegType.SØKNAD) { _, kontekst, _ ->
                Tidslinje(
                    kontekst.rettighetsperiode,
                    if (trukketSøknadService.søknadErTrukket(kontekst.behandlingId))
                        TidligereVurderinger.IkkeBehandlingsgrunnlag
                    else
                        TidligereVurderinger.PotensieltOppfylt(null)
                )
            },

            Sjekk(StegType.VURDER_LOVVALG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.LOVVALG, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_ALDER) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.ALDERSVILKÅRET, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_BISTANDSBEHOV) { _, kontekst, _ ->
                /* TODO: Tror ikke dette er riktig. Sykdomsvilkåret er ikke satt når
                *   man er i steget VURDER_BiSTANDSBEHOV. */
                val periode = kontekst.rettighetsperiode
                val sykdomstidslinje = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.somSykdomsvurderingstidslinje().orEmpty()
                val studenttidslinje =
                    studentRepository.hentHvisEksisterer(kontekst.behandlingId)?.somStudenttidslinje(periode.tom)
                        .orEmpty()

                sykdomstidslinje.outerJoin(studenttidslinje) { segmentPeriode, sykdomsvurdering, studentVurdering ->
                    if (studentVurdering != null && studentVurdering.erOppfylt()) return@outerJoin TidligereVurderinger.PotensieltOppfylt(
                        null
                    )

                    val erIkkeFørsteSykdomsvurdering =
                        !Sykdomsvurdering.erFørsteVurdering(kontekst.rettighetsperiode.fom, segmentPeriode)

                    val harTidligereInnvilgetSykdomsvurdering by lazy {
                        sykdomstidslinje
                            .begrensetTil(Periode(Tid.MIN, segmentPeriode.fom.minusDays(1)))
                            .segmenter()
                            .any { it.verdi.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet() }
                    }

                    if (erIkkeFørsteSykdomsvurdering && harTidligereInnvilgetSykdomsvurdering) {
                        return@outerJoin TidligereVurderinger.PotensieltOppfylt(null)
                    }

                    val sykdomDefinitivtAvslag =
                        sykdomsvurdering?.erOppfyltOrdinærSettBortIfraVissVarighet() == false
                                && !sykdomsvurdering.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet()

                    if (sykdomDefinitivtAvslag) {
                        return@outerJoin TidligereVurderinger.UunngåeligAvslag
                    }

                    return@outerJoin TidligereVurderinger.PotensieltOppfylt(null)
                }
            },


            Sjekk(StegType.VURDER_SYKEPENGEERSTATNING) { vilkårsresultat, kontekst, _ ->

                val sykdomVurdering = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
                val sykdomstidslinje = sykdomVurdering?.somSykdomsvurderingstidslinje().orEmpty()
                val yrkesskaderTidslinje =
                    sykdomVurdering?.yrkesskadevurdringTidslinje(kontekst.rettighetsperiode).orEmpty()
                val bistandTidslinje =
                    bistandRepository.hentHvisEksisterer(kontekst.behandlingId)?.somBistandsvurderingstidslinje()
                        .orEmpty()
                val overgangUføre1118 = vilkårsresultat.tidslinjeFor(Vilkårtype.OVERGANGUFØREVILKÅRET)
                val overgangArbeid1117 = vilkårsresultat.tidslinjeFor(Vilkårtype.OVERGANGARBEIDVILKÅRET)
                val sykdomErstating1113 = vilkårsresultat.tidslinjeFor(Vilkårtype.SYKEPENGEERSTATNING)

                Tidslinje.map6(
                    sykdomstidslinje,
                    yrkesskaderTidslinje,
                    bistandTidslinje,
                    overgangUføre1118,
                    overgangArbeid1117,
                    sykdomErstating1113
                ) { sykdomVurdering115Segment,
                    yrkesskaderVudering1122Segment,
                    bistandsVurdering116Segment,
                    overgangUføre1118VilkårsSegment,
                    overgangArbeid1117VilkårSegment,
                    sykdomErstating1113VilkårSegment ->

                    val sykdomOppfylt = (sykdomVurdering115Segment?.harSkadeSykdomEllerLyte == true
                            && sykdomVurdering115Segment.erArbeidsevnenNedsatt == true
                            && (sykdomVurdering115Segment.erNedsettelseIArbeidsevneMerEnnHalvparten == true
                            || sykdomVurdering115Segment.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true)
                            && sykdomVurdering115Segment.erSkadeSykdomEllerLyteVesentligdel == true
                            && (sykdomVurdering115Segment.erNedsettelseIArbeidsevneAvEnVissVarighet == true
                            || sykdomVurdering115Segment.erNedsettelseIArbeidsevneAvEnVissVarighet == null))

                    val bistandOppfylt = (bistandsVurdering116Segment?.erBehovForAktivBehandling == true
                            || bistandsVurdering116Segment?.erBehovForArbeidsrettetTiltak == true) ||
                            (bistandsVurdering116Segment?.erBehovForAnnenOppfølging == true)

                    val førerTilAvslag = when {
                        // ja 115, nei 116, nei 1118, nei/ikke vudert 1117, nei 1113
                        sykdomOppfylt
                                && !bistandOppfylt
                                && overgangUføre1118VilkårsSegment?.utfall == IKKE_OPPFYLT
                                && overgangArbeid1117VilkårSegment?.utfall != Utfall.OPPFYLT
                                && sykdomErstating1113VilkårSegment?.utfall == IKKE_OPPFYLT -> true

                        //nei,vis varigghet
                        sykdomVurdering115Segment?.harSkadeSykdomEllerLyte == true
                                && sykdomVurdering115Segment.erArbeidsevnenNedsatt == true
                                && sykdomVurdering115Segment.erNedsettelseIArbeidsevneMerEnnHalvparten == true
                                && sykdomVurdering115Segment.erSkadeSykdomEllerLyteVesentligdel == true
                                && sykdomVurdering115Segment.erNedsettelseIArbeidsevneAvEnVissVarighet == false
                                && sykdomErstating1113VilkårSegment?.utfall == IKKE_OPPFYLT -> true

                        //YS 11-22 veien til avslag
                        sykdomOppfylt && sykdomVurdering115Segment.erNedsettelseIArbeidsevneMerEnnHalvparten == false
                                && sykdomVurdering115Segment.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == true
                                && bistandOppfylt && yrkesskaderTidslinje.filter { it.verdi.erÅrsakssammenheng }
                            .isEmpty()
                            -> true
                        // nei 115, ikke vurdert/ikke oppfylt1117, nei 1113
                        !sykdomOppfylt
                                && sykdomErstating1113VilkårSegment?.utfall == IKKE_OPPFYLT
                                && overgangArbeid1117VilkårSegment?.utfall != Utfall.OPPFYLT -> true

                        else -> false
                    }
                    if (førerTilAvslag) TidligereVurderinger.UunngåeligAvslag else TidligereVurderinger.PotensieltOppfylt(
                        null
                    )
                }
            },

            Sjekk(StegType.FASTSETT_SYKDOMSVILKÅRET) { _, _, _ ->
                /* Det finnes unntak til sykdomsvilkåret, så selv om vilkåret ikke er oppfylt, så
                 * vet vi ikke her om det blir avslag eller ei. */
                Tidslinje()
            },

            Sjekk(StegType.FASTSETT_GRUNNLAG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.GRUNNLAGET, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_INNTEKTSBORTFALL) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.INNTEKTSBORTFALL, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_MEDLEMSKAP) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.MEDLEMSKAP, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_AVSLAG) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_SYKESTIPEND) { vilkårsresultat, _, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING, vilkårsresultat)
            },
        )
    )

    private fun lagSjekker(definerteSjekker: List<Sjekk>) = buildList {
        val førstegangsbehandling = Førstegangsbehandling.flyt()
        definerteSjekker.windowed(2).forEach { (sjekk1, sjekk2) ->
            require(førstegangsbehandling.erStegFør(sjekk1.steg, sjekk2.steg)) {
                "Koden for avslag-logikk forutsetter at ${sjekk1.steg} kommer før ${sjekk2.steg} i flyten, noe som ikke stemmer."
            }
        }

        val sjekker = definerteSjekker.iterator()
        var sjekk: Sjekk? = sjekker.next()

        /* legg på default sjekk der det mangler. */
        for (steg in Førstegangsbehandling.flyt().stegene()) {
            if (steg == sjekk?.steg) {
                add(sjekk)
                sjekk = if (sjekker.hasNext()) sjekker.next() else null
            } else {
                add(Sjekk(steg) { _, _, _ -> Tidslinje() })
            }
        }
        check(sjekk == null) { "sjekk ${sjekk?.steg} plasser ikke inn i flyten" }
        check(!sjekker.hasNext()) { "sjekk ${sjekker.next().steg} plasser ikke inn i flyten" }
    }

    private fun gir(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): TidligereVurderinger.Behandlingsutfall {
        val utfall = behandlingsutfall(kontekst, førSteg)
        return when {
            utfall.isEmpty() || !utfall.erSammenhengende() -> TidligereVurderinger.PotensieltOppfylt(null)
            utfall.helePerioden() != kontekst.rettighetsperiode -> TidligereVurderinger.PotensieltOppfylt(null)

            utfall.segmenter()
                .any { it.verdi == TidligereVurderinger.IkkeBehandlingsgrunnlag } -> TidligereVurderinger.IkkeBehandlingsgrunnlag.also {
                log.info(
                    "Gir TidligereVurderinger.IkkeBehandlingsgrunnlag i steg: $førSteg."
                )
            }

            utfall.segmenter()
                .all { it.verdi == TidligereVurderinger.UunngåeligAvslag } -> TidligereVurderinger.UunngåeligAvslag.also {
                log.info(
                    "Gir avslag for TidligereVurderinger.UunngåeligAvslag i steg: $førSteg."
                )
            }

            else -> TidligereVurderinger.PotensieltOppfylt(null)
        }
    }


    override fun behandlingsutfall(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        val sjekker = when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling,
            TypeBehandling.Revurdering -> when (unleashGateway.isEnabled(BehandlingsflytFeature.NyTidligereVurderinger)) {
                true -> sjekker
                false -> gamleSjekker
            }

            else -> return tidslinjeOf(
                kontekst.rettighetsperiode to TidligereVurderinger.PotensieltOppfylt(null)
            )
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        return sjekker
            .asSequence()
            .takeWhile { it.steg != førSteg }
            .fold(
                tidslinjeOf(kontekst.rettighetsperiode to TidligereVurderinger.PotensieltOppfylt(null) as TidligereVurderinger.Behandlingsutfall)
            ) { foreløpigTidslinje, sjekk ->
                foreløpigTidslinje.leftJoin(
                    sjekk.sjekk(
                        vilkårsresultat,
                        kontekst,
                        foreløpigTidslinje
                    )
                ) { foreløpigUtfall, nesteUtfall ->
                    when {
                        nesteUtfall == null -> foreløpigUtfall
                        foreløpigUtfall is TidligereVurderinger.IkkeBehandlingsgrunnlag || nesteUtfall is TidligereVurderinger.IkkeBehandlingsgrunnlag -> TidligereVurderinger.IkkeBehandlingsgrunnlag
                        foreløpigUtfall is TidligereVurderinger.UunngåeligAvslag || nesteUtfall is TidligereVurderinger.UunngåeligAvslag -> TidligereVurderinger.UunngåeligAvslag
                        foreløpigUtfall is TidligereVurderinger.PotensieltOppfylt && nesteUtfall is TidligereVurderinger.PotensieltOppfylt -> TidligereVurderinger.PotensieltOppfylt(
                            foreløpigUtfall.rettighetstype ?: nesteUtfall.rettighetstype
                        )

                        else -> error("Uventet kombinasjon av utfall: $foreløpigUtfall og $nesteUtfall")
                    }

                }
            }
            .begrensetTil(kontekst.rettighetsperiode)

    }

    override fun girAvslagEllerIngenBehandlingsgrunnlag(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): Boolean {
        return (gir(kontekst, førSteg) in listOf(
            TidligereVurderinger.IkkeBehandlingsgrunnlag,
            TidligereVurderinger.UunngåeligAvslag
        ))
    }

    override fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(kontekst, førSteg) == TidligereVurderinger.UunngåeligAvslag)
    }


    override fun girIngenBehandlingsgrunnlag(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): Boolean {
        return (gir(
            kontekst,
            førSteg
        ) == TidligereVurderinger.IkkeBehandlingsgrunnlag)
    }

    private fun ikkeOppfyltFørerTilAvslag(
        vilkårtype: Vilkårtype,
        vilkårsresultat: Vilkårsresultat,
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        return vilkårsresultat.tidslinjeFor(vilkårtype)
            .mapValue {
                when (it.utfall) {
                    IKKE_OPPFYLT -> TidligereVurderinger.UunngåeligAvslag
                    else -> TidligereVurderinger.PotensieltOppfylt(null)
                }
            }
    }

    private fun potensieltOppfyltOvergangArbeid(
        rettighetsperiode: Periode,
        segmentPeriode: Periode,
        sykdomstidslinje: Tidslinje<Sykdomsvurdering>
    ): Boolean {
        val harTidligereInnvilgetSykdomsvurdering =
            sykdomstidslinje
                .begrensetTil(Periode(Tid.MIN, segmentPeriode.fom.minusDays(1)))
                .segmenter()
                .any { it.verdi.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet() }

        val erIkkeFørsteSykdomsvurdering =
            !Sykdomsvurdering.erFørsteVurdering(rettighetsperiode.fom, segmentPeriode)
        return erIkkeFørsteSykdomsvurdering && harTidligereInnvilgetSykdomsvurdering
    }
}
