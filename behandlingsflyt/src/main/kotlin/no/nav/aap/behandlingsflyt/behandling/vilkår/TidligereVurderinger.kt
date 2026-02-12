package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger.Behandlingsutfall.IKKE_BEHANDLINGSGRUNNLAG
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger.Behandlingsutfall.UKJENT
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger.Behandlingsutfall.UUNGÅELIG_AVSLAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

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

    enum class Behandlingsutfall {
        IKKE_BEHANDLINGSGRUNNLAG,
        UUNGÅELIG_AVSLAG,
        UKJENT,
    }

    fun behandlingsutfall(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Tidslinje<Behandlingsutfall>
}

class TidligereVurderingerImpl(
    private val trukketSøknadService: TrukketSøknadService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val sykdomRepository: SykdomRepository,
    private val studentRepository: StudentRepository,
    private val bistandRepository: BistandRepository,
    private val yrkesskadeRepository: YrkesskadeRepository,
) : TidligereVurderinger {

    private val log = LoggerFactory.getLogger(javaClass)


    constructor(
        repositoryProvider: RepositoryProvider,
    ) : this(
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        sykdomRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        yrkesskadeRepository = repositoryProvider.provide(),
    )

    data class Sjekk(
        val steg: StegType,
        val sjekk: (vilkårsresultat: Vilkårsresultat, kontekst: FlytKontekstMedPerioder) -> Tidslinje<TidligereVurderinger.Behandlingsutfall>
    )

    private fun definerteSjekker(typeBehandling: TypeBehandling): List<Sjekk> {
        val spesifikkeSjekker = when (typeBehandling) {
            TypeBehandling.Revurdering -> listOf(
                Sjekk(StegType.AVBRYT_REVURDERING) { _, kontekst ->
                    Tidslinje(
                        kontekst.rettighetsperiode,
                        if (avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId))
                            IKKE_BEHANDLINGSGRUNNLAG
                        else
                            UKJENT
                    )
                }
            )

            TypeBehandling.Førstegangsbehandling -> listOf(
                Sjekk(StegType.SØKNAD) { _, kontekst ->
                    Tidslinje(
                        kontekst.rettighetsperiode,
                        if (trukketSøknadService.søknadErTrukket(kontekst.behandlingId))
                            IKKE_BEHANDLINGSGRUNNLAG
                        else
                            UKJENT
                    )
                }
            )

            else -> emptyList()
        }

        val fellesSjekker = listOf(
            Sjekk(StegType.VURDER_LOVVALG) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.LOVVALG, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_ALDER) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.ALDERSVILKÅRET, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_BISTANDSBEHOV) { _, kontekst ->
                /* TODO: Tror ikke dette er riktig. Sykdomsvilkåret er ikke satt når
                *   man er i steget VURDER_BiSTANDSBEHOV. */
                val periode = kontekst.rettighetsperiode
                val sykdomstidslinje = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
                    ?.somSykdomsvurderingstidslinje().orEmpty()
                val studenttidslinje =
                    studentRepository.hentHvisEksisterer(kontekst.behandlingId)?.somStudenttidslinje(periode.tom).orEmpty()

                sykdomstidslinje.outerJoin(studenttidslinje) { segmentPeriode, sykdomsvurdering, studentVurdering ->
                    if (studentVurdering != null && studentVurdering.erOppfylt()) return@outerJoin UKJENT

                    val erIkkeFørsteSykdomsvurdering =
                        !Sykdomsvurdering.erFørsteVurdering(kontekst.rettighetsperiode.fom, segmentPeriode)

                    val harTidligereInnvilgetSykdomsvurdering by lazy {
                        sykdomstidslinje
                            .begrensetTil(Periode(Tid.MIN, segmentPeriode.fom.minusDays(1)))
                            .segmenter()
                            .any { it.verdi.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet() }
                    }

                    if (erIkkeFørsteSykdomsvurdering && harTidligereInnvilgetSykdomsvurdering) {
                        return@outerJoin UKJENT
                    }

                    val sykdomDefinitivtAvslag =
                        sykdomsvurdering?.erOppfyltOrdinærSettBortIfraVissVarighet() == false
                                && !sykdomsvurdering.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengOgVissVarighet()

                    if (sykdomDefinitivtAvslag) {
                        return@outerJoin UUNGÅELIG_AVSLAG
                    }

                    return@outerJoin UKJENT
                }
            },


            Sjekk(StegType.VURDER_SYKEPENGEERSTATNING) { vilkårsresultat, kontekst ->

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
                    if (førerTilAvslag) UUNGÅELIG_AVSLAG else UKJENT
                }
            },

            Sjekk(StegType.FASTSETT_SYKDOMSVILKÅRET) { _, _ ->
                /* Det finnes unntak til sykdomsvilkåret, så selv om vilkåret ikke er oppfylt, så
                 * vet vi ikke her om det blir avslag eller ei. */
                Tidslinje()
            },

            Sjekk(StegType.FASTSETT_GRUNNLAG) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.GRUNNLAGET, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_INNTEKTSBORTFALL) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.INNTEKTSBORTFALL, vilkårsresultat)
            },

            Sjekk(StegType.VURDER_MEDLEMSKAP) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.MEDLEMSKAP, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_AVSLAG) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING, vilkårsresultat)
            },

            Sjekk(StegType.SAMORDNING_SYKESTIPEND) { vilkårsresultat, _ ->
                ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING, vilkårsresultat)
            },
        )
        return spesifikkeSjekker + fellesSjekker
    }

    init {
        val førstegangsbehandling = Førstegangsbehandling.flyt()
        definerteSjekker(TypeBehandling.Førstegangsbehandling).windowed(2).forEach { (sjekk1, sjekk2) ->
            require(førstegangsbehandling.erStegFør(sjekk1.steg, sjekk2.steg)) {
                "Avslag-logikk forutsetter at ${sjekk1.steg} kommer før ${sjekk2.steg} i førstegangsbehandling-flyten."
            }
        }

        var sjekkerRevurdering = Revurdering.flyt()
        definerteSjekker(TypeBehandling.Revurdering).windowed(2).forEach { (sjekk1, sjekk2) ->
            require(sjekkerRevurdering.erStegFør(sjekk1.steg, sjekk2.steg)) {
                "Avslag-logikk forutsetter at ${sjekk1.steg} kommer før ${sjekk2.steg} i revurdering-flyten."
            }
        }
    }

    private var sjekkerRevurdering = lagSjekker(definerteSjekker(TypeBehandling.Revurdering))
    private var sjekkerFørstegangsbehandling = lagSjekker(definerteSjekker(TypeBehandling.Førstegangsbehandling))

    private fun lagSjekker(definerteSjekker: List<Sjekk>) = buildList {
        val sjekker = definerteSjekker.iterator()
        var sjekk: Sjekk? = sjekker.next()

        /* legg på default sjekk der det mangler. */
        for (steg in Førstegangsbehandling.flyt().stegene()) {
            if (steg == sjekk?.steg) {
                add(sjekk)
                sjekk = if (sjekker.hasNext()) sjekker.next() else null
            } else {
                add(Sjekk(steg) { _, _ -> Tidslinje() })
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
            utfall.isEmpty() || !utfall.erSammenhengende() -> UKJENT
            utfall.helePerioden() != kontekst.rettighetsperiode -> UKJENT
            utfall.segmenter().any { it.verdi == IKKE_BEHANDLINGSGRUNNLAG } -> IKKE_BEHANDLINGSGRUNNLAG.also {
                log.info(
                    "Gir IKKE_BEHANDLINGSGRUNNLAG i steg: $førSteg."
                )
            }

            utfall.segmenter()
                .all { it.verdi == UUNGÅELIG_AVSLAG } -> UUNGÅELIG_AVSLAG.also { log.info("Gir avslag for UUNGÅELIG_AVSLAG i steg: $førSteg.") }

            else -> UKJENT
        }
    }


    override fun behandlingsutfall(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        val sjekker = when (kontekst.behandlingType) {
            TypeBehandling.Førstegangsbehandling -> sjekkerFørstegangsbehandling
            TypeBehandling.Revurdering -> sjekkerRevurdering
            else -> return tidslinjeOf(kontekst.rettighetsperiode to UKJENT)
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        return sjekker
            .asSequence()
            .takeWhile { it.steg != førSteg }
            .map { it.sjekk(vilkårsresultat, kontekst) }
            .plus(listOf(tidslinjeOf(kontekst.rettighetsperiode to UKJENT)))
            .asIterable()
            .outerJoin { it.minOrNull() ?: UKJENT }
            .begrensetTil(kontekst.rettighetsperiode)
    }

    override fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(kontekst, førSteg) in listOf(
            IKKE_BEHANDLINGSGRUNNLAG,
            UUNGÅELIG_AVSLAG
        ))
    }

    override fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(kontekst, førSteg) == UUNGÅELIG_AVSLAG)
    }


    override fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(
            kontekst,
            førSteg
        ) == IKKE_BEHANDLINGSGRUNNLAG)
    }

    private fun ikkeOppfyltFørerTilAvslag(
        vilkårtype: Vilkårtype,
        vilkårsresultat: Vilkårsresultat,
    ): Tidslinje<TidligereVurderinger.Behandlingsutfall> {
        return vilkårsresultat.tidslinjeFor(vilkårtype)
            .mapValue {
                when (it.utfall) {
                    IKKE_OPPFYLT -> UUNGÅELIG_AVSLAG
                    else -> UKJENT
                }
            }
    }
}
