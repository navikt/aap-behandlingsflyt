package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.UUNGÅELIG_AVSLAG
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.IKKE_BEHANDLINGSGRUNNLAG
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl.UtfallForFørstegangsbehandling.UKJENT
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider

/** Når kan vi definitivt si at det er avslag, slik
 * at vi ikke trenger å vurdere flere vilkår.
 *
 * Ved revurdering, er det viktig at vi kun ser fram til aktivt steg,
 * fordi hvis det er avslag på et steg senere i flyten, så kan det være
 * at den vurderingen endres slik at det ikke lenger er et avslag.
 */
interface TidligereVurderinger {
    fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean

    fun harBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return !girIngenBehandlingsgrunnlag(kontekst, førSteg)
    }
}

class TidligereVurderingerImpl(
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
) : TidligereVurderinger {
    /* Kan være vi kan generalisere til revurdering også, men begynner med førstegangsbehandling. */
    enum class UtfallForFørstegangsbehandling {
        IKKE_BEHANDLINGSGRUNNLAG,
        UUNGÅELIG_AVSLAG,
        UKJENT,
    }

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
    )


    class Sjekk(
        val steg: StegType,
        val sjekk: (kontekst: FlytKontekstMedPerioder) -> UtfallForFørstegangsbehandling
    )

    private val definerteSjekkerFørstegangsbehandling = listOf(
        Sjekk(StegType.SØKNAD) {
            if (trukketSøknadService.søknadErTrukket(it.behandlingId))
                IKKE_BEHANDLINGSGRUNNLAG
            else
                UKJENT
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
                add(Sjekk(steg) {
                    UKJENT
                })
            }
        }
        check(sjekk == null)
        check(!sjekker.hasNext())
    }

    private fun gir(
        kontekst: FlytKontekstMedPerioder,
        førSteg: StegType
    ): UtfallForFørstegangsbehandling {
        if (Miljø.erProd()) {
            return UKJENT
        }

        if (kontekst.behandlingType != TypeBehandling.Førstegangsbehandling) {
            return UKJENT
        }

        return sjekkerFørstegangsbehandling
            .asSequence()
            .takeWhile { it.steg != førSteg }
            .map { it.sjekk(kontekst) }
            .firstOrNull { it != UKJENT }
            ?: UKJENT

    }

    override fun girAvslagEllerIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return gir(kontekst, førSteg) in listOf(IKKE_BEHANDLINGSGRUNNLAG, UUNGÅELIG_AVSLAG)
    }

    override fun girIngenBehandlingsgrunnlag(kontekst: FlytKontekstMedPerioder, førSteg: StegType): Boolean {
        return gir(kontekst, førSteg) in listOf(IKKE_BEHANDLINGSGRUNNLAG)
    }
}