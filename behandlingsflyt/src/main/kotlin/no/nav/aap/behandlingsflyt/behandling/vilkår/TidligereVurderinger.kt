package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.IKKE_BEHANDLINGSGRUNNLAG
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.UKJENT
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.UUNGÅELIG_AVSLAG
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.IKKE_OPPFYLT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoin
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

/** Når kan vi definitivt si at det er avslag, slik
 * at vi ikke trenger å vurdere flere vilkår.
 *
 * Ved revurdering, er det viktig at vi kun ser fram til aktivt steg,
 * fordi selv om det er avslag på et steg senere i flyten, så kan det være
 * at den vurderingen endres slik at det ikke lenger er et avslag.
 */
interface TidligereVurderinger {
    fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean
}

class TidligereVurderingerImpl(
    private val trukketSøknadService: TrukketSøknadService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) : TidligereVurderinger {

    private val log = LoggerFactory.getLogger(javaClass)

    /* Kan være vi kan generalisere til revurdering også, men begynner med førstegangsbehandling. */
    enum class UtfallForFørstegangsbehandling {
        IKKE_BEHANDLINGSGRUNNLAG,
        UUNGÅELIG_AVSLAG,
        UKJENT,
    }

    constructor(repositoryProvider: RepositoryProvider) : this(
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    class Sjekk(
        val steg: StegType,
        val sjekk: (vilkårsresultat: Vilkårsresultat, kontekst: FlytKontekstMedPerioder) -> Tidslinje<UtfallForFørstegangsbehandling>
    )

    private val definerteSjekkerFørstegangsbehandling = listOf(
        Sjekk(StegType.SØKNAD) { _, kontekst ->
            Tidslinje(
                kontekst.rettighetsperiode,
                if (trukketSøknadService.søknadErTrukket(kontekst.behandlingId))
                    IKKE_BEHANDLINGSGRUNNLAG
                else
                    UKJENT
            )
        },

        Sjekk(StegType.VURDER_LOVVALG) { vilkårsresultat, _ ->
            ikkeOppfyltFørerTilAvslag(Vilkårtype.LOVVALG, vilkårsresultat)
        },

        Sjekk(StegType.VURDER_ALDER) { vilkårsresultat, _ ->
            ikkeOppfyltFørerTilAvslag(Vilkårtype.ALDERSVILKÅRET, vilkårsresultat)
        },

        Sjekk(StegType.VURDER_BISTANDSBEHOV) { vilkårsresultat, _ ->
            val sykdomstidslinje = vilkårsresultat.tidslinjeFor(Vilkårtype.SYKDOMSVILKÅRET)
            val bistandstidslinje = vilkårsresultat.tidslinjeFor(Vilkårtype.BISTANDSVILKÅRET)

            sykdomstidslinje.outerJoin(bistandstidslinje) { sykdomsvilkåret, bistandsvilkåret ->
                val sykdomAvslagPgaVarighet =
                    sykdomsvilkåret?.utfall == IKKE_OPPFYLT && sykdomsvilkåret.avslagsårsak == Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
                when {
                    sykdomsvilkåret?.utfall == IKKE_OPPFYLT && !sykdomAvslagPgaVarighet -> UUNGÅELIG_AVSLAG
                    sykdomAvslagPgaVarighet && bistandsvilkåret?.utfall == IKKE_OPPFYLT -> UUNGÅELIG_AVSLAG
                    else -> UKJENT
                }
            }
        },

        Sjekk(StegType.VURDER_SYKEPENGEERSTATNING) { vilkårsresultat, _ ->
            val sykdomstidslinje = vilkårsresultat.tidslinjeFor(Vilkårtype.SYKDOMSVILKÅRET)
            val erstatningstidslinje = vilkårsresultat.tidslinjeFor(Vilkårtype.BISTANDSVILKÅRET)
                .filter { it.verdi.innvilgelsesårsak == Innvilgelsesårsak.SYKEPENGEERSTATNING }

            val bistandTidslinje = vilkårsresultat.tidslinjeFor(Vilkårtype.BISTANDSVILKÅRET)

            Tidslinje.zip3(sykdomstidslinje, erstatningstidslinje, bistandTidslinje)
                .mapValue { (sykdomsvilkåret, sykepengeerstatning, bistand) ->
                    when {
                        sykdomsvilkåret?.utfall == IKKE_OPPFYLT && sykepengeerstatning?.utfall == IKKE_OPPFYLT -> UUNGÅELIG_AVSLAG
                        else -> UKJENT
                    }
                }
        },

        Sjekk(StegType.FASTSETT_SYKDOMSVILKÅRET) { vilkårsresultat, _ ->
            /* Det finnes unntak til sykdomsvilkåret, så selv om vilkåret ikke er oppfylt, så
             * vet vi ikke her om det blir avslag eller ei. */
            Tidslinje()
        },

        Sjekk(StegType.FASTSETT_GRUNNLAG) { vilkårsresultat, _ ->
            ikkeOppfyltFørerTilAvslag(Vilkårtype.GRUNNLAGET, vilkårsresultat)
        },

        Sjekk(StegType.VURDER_MEDLEMSKAP) { vilkårsresultat, _ ->
            ikkeOppfyltFørerTilAvslag(Vilkårtype.MEDLEMSKAP, vilkårsresultat)
        },

        Sjekk(StegType.SAMORDNING_AVSLAG) { vilkårsresultat, _ ->
            ikkeOppfyltFørerTilAvslag(Vilkårtype.SAMORDNING, vilkårsresultat)
        },
    )


    init {
        val førstegangsbehandling = Førstegangsbehandling.flyt()
        definerteSjekkerFørstegangsbehandling.windowed(2).forEach { (sjekk1, sjekk2) ->
            require(førstegangsbehandling.erStegFør(sjekk1.steg, sjekk2.steg)) {
                "Avslag-logikk forutsetter at ${sjekk1.steg} kommer før ${sjekk2.steg} i førstegangsbehandling-flyten."
            }
        }
    }

    private var sjekkerFørstegangsbehandling = buildList {
        val sjekker = definerteSjekkerFørstegangsbehandling.iterator()
        var sjekk: Sjekk? = sjekker.next()

        /* legg på default sjekk der det mangler. */
        for (steg in Førstegangsbehandling.flyt().stegene()) {
            if (steg == sjekk?.steg) {
                add(sjekk)
                sjekk = if (sjekker.hasNext()) sjekker.next() else null
            } else {
                add(Sjekk(steg) { _, _ ->
                    Tidslinje()
                })
            }
        }
        check(sjekk == null) {
            "sjekk ${sjekk?.steg} plasser ikke inn i flyten"
        }
        check(!sjekker.hasNext()) {
            "sjekk ${sjekker.next().steg} plasser ikke inn i flyten"
        }
    }

    private fun gir(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): UtfallForFørstegangsbehandling {
        if (kontekst.behandlingType != TypeBehandling.Førstegangsbehandling) {
            return UKJENT
        }

        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)

        val utfall = sjekkerFørstegangsbehandling
            .asSequence()
            .takeWhile { it.steg != førSteg }
            .map { it.sjekk(vilkårsresultat, kontekst) }
            .asIterable()
            .outerJoin { it.minOrNull() ?: UKJENT }
            .begrensetTil(kontekst.rettighetsperiode)

        return when {
            utfall.isEmpty() || !utfall.erSammenhengende() -> UKJENT
            utfall.helePerioden() != kontekst.rettighetsperiode -> UKJENT
            utfall.any { it.verdi == IKKE_BEHANDLINGSGRUNNLAG } -> IKKE_BEHANDLINGSGRUNNLAG
            utfall.all { it.verdi == UUNGÅELIG_AVSLAG } -> UUNGÅELIG_AVSLAG
            else -> UKJENT
        }
    }

    override fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(kontekst, førSteg) in listOf(
            IKKE_BEHANDLINGSGRUNNLAG,
            UUNGÅELIG_AVSLAG
        ))
    }

    override fun girAvslag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(kontekst, førSteg) == UUNGÅELIG_AVSLAG).also {
            if (it) {
                log.info("Gir avslag i steg: $førSteg.")
            }
        }
    }


    override fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return (gir(
            kontekst,
            førSteg
        ) == IKKE_BEHANDLINGSGRUNNLAG).also {
            if (it) {
                log.info("Gir ingen behandlingsgrunnlag i steg: $førSteg.")
            }
        }
    }

    private fun ikkeOppfyltFørerTilAvslag(
        vilkårtype: Vilkårtype,
        vilkårsresultat: Vilkårsresultat,
    ): Tidslinje<UtfallForFørstegangsbehandling> {
        return vilkårsresultat.tidslinjeFor(vilkårtype)
            .mapValue {
                when (it.utfall) {
                    IKKE_OPPFYLT -> UUNGÅELIG_AVSLAG
                    else -> UKJENT
                }
            }
    }
}
