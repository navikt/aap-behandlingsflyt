package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVBRUTT
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.KVALITETSSIKRET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.TOTRINNS_VURDERT
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.repository.RepositoryProvider

class EnkeltAvklaringsbehovstegService(
    private val avbrytRevurderingService: AvbrytRevurderingService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)
    )


    /** Oppdater tilstanden på avklaringsbehovet [definisjon], slik at kvalitetssikring,
     * to-trinnskontroll og tilbakeflyt blir riktig.
     *
     * For at kvalitetssikring og totrinnskontroll vises for riktig steg, så er det
     * viktig at avklaringsbehovet har rett status. Ved å bruke denne funksjonen
     * ivaretar man det.
     *
     * For at flyten skal bli riktig hvis man beveger seg fram og tilbake i flyten,
     * så er det viktig at et steg rydder opp etter seg når det viser seg at steget
     * ikke er relevant allikevel. Denne funksjonen hjelper også med det.
     */
    fun oppdaterAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        definisjon: Definisjon,

        /** Skal vedtaket inneholde en menneskelig vurdering av [definisjon]?
         *
         * Det er viktig å svare på det mer generelle spørsmålet *om vedtaket*
         * skal inneholde en menneskelig vurdering. Ikke om nå-tilstanden av behandlingen
         * har behov for en menneskelig vurdering. Grunnen er at det vil være behov for totrinnskontroll hvis vedtaket inneholder
         * en menneskelig vurdering, selv om siste gjennomkjøring av steget
         * ikke løftet avklaringsbehovet.
         *
         * En egenskap denne funksjonen må ha:
         * Hvis `vedtakBehøverVurdering() == true` og noen løser
         * (avklaringsbehovet)[definisjon], så er fortsatt `vedtakBehøverVurdering() == true`.
         *
         * @return Skal returnere `true` hvis behandlingen kommer til å inneholde
         * en menneskelig vurdering av [definisjon].
         */
        vedtakBehøverVurdering: () -> Boolean,


        /** Er avklaringsbehovet [definisjon] tilstrekkelig vurdert for å fortsette behandlingen?
         *
         * Denne funksjonen kalles kun om `vedtakBehøverVurdering() == true` og avklaringsbehovet
         * [definisjon] allerede har en løsning. Merk at selv om definisjonen allerede har en løsning,
         * så kan den løsningen ha blitt rullet tilbake (se [tilbakestillGrunnlag]).
         */
        erTilstrekkeligVurdert: () -> Boolean,

        /** Rydd opp manuelle vurderinger introdusert i denne behandlingen på grunn av løsninger
         * av avklaringsbehovet [definisjon].
         *
         * - Du burde ikke rydde opp for andre steg eller avklaringsbehov.
         * - Hvis register-data og menneskelige vurderinger er lagret i samme grunnlag, så pass
         *   på at du ikke tilbakestiller register-dataen!
         */
        tilbakestillGrunnlag: () -> Unit,
        kontekst: FlytKontekstMedPerioder
    ) {
        require(definisjon.løsesISteg != StegType.UDEFINERT)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        if (vedtakBehøverVurdering()) {
            if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken() || avklaringsbehov.status() == AVBRUTT) {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov?.status()) {
                    OPPRETTET -> {
                        /* ønsket tilstand er OPPRETTET */
                    }

                    null, AVBRUTT ->
                        avklaringsbehovene.leggTil(listOf(definisjon), definisjon.løsesISteg)

                    TOTRINNS_VURDERT,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    AVSLUTTET ->
                        error("ikke mulig")
                }
            } else if (erTilstrekkeligVurdert()) {
                /* ønsket tilstand: ... */
                when (avklaringsbehov.status()) {
                    OPPRETTET, AVBRUTT ->
                        avklaringsbehovene.avslutt(definisjon)

                    AVSLUTTET,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    TOTRINNS_VURDERT -> {
                        /* uendret status */
                    }
                }
            } else {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov.status()) {
                    OPPRETTET -> {
                        /* forbli OPPRETTET */
                    }

                    AVSLUTTET,
                    TOTRINNS_VURDERT,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    AVBRUTT -> {
                        avklaringsbehovene.leggTil(listOf(definisjon), definisjon.løsesISteg)
                    }
                }
            }
        } else /* vedtaket behøver ikke vurdering */ {
            /* ønsket tilstand: ikke eksistere (null) eller AVBRUTT. */
            when (avklaringsbehov?.status()) {
                null,
                AVBRUTT -> {
                    /* allerede ønsket tilstand */
                }

                OPPRETTET,
                AVSLUTTET,
                TOTRINNS_VURDERT,
                SENDT_TILBAKE_FRA_BESLUTTER,
                KVALITETSSIKRET,
                SENDT_TILBAKE_FRA_KVALITETSSIKRER -> {
                    avklaringsbehovene.avbryt(definisjon)
                    if (!avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)) {
                        tilbakestillGrunnlag()
                    }
                }
            }
        }
    }

    /** Special case av [oppdaterAvklaringsbehov] for vilkår som er periodisert. Brukeren
     * av funksjonen må fortelle hvilke perioder hvor vilkåret kan bli vurdert for ([nårVurderingErRelevant]).
     *
     * Hvis det er en periode som trenger vurdering som ikke trengte vurdering i forrige behandling, så løftes
     * avklaringsbehovet.
     *
     * Hvis vurderingsbehovene relevant for steget er i [tvingerAvklaringsbehov], så åpnes avklaringsbehovet
     * også hvis det ikke er en endring i periodene som behøver vurdering, gitt at det er noen perioder som
     * behøver vurdering.
     *
     * Vurder å skrive om til service, slik at man slipper å injecte inn alle repositoriesene?
     */
    fun oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
        avklaringsbehovene: Avklaringsbehovene,
        behandlingRepository: BehandlingRepository,
        vilkårsresultatRepository: VilkårsresultatRepository,
        definisjon: Definisjon,
        tvingerAvklaringsbehov: Set<Vurderingsbehov>,
        nårVurderingErRelevant: (kontekst: FlytKontekstMedPerioder) -> Tidslinje<Boolean>,
        kontekst: FlytKontekstMedPerioder,
        erTilstrekkeligVurdert: () -> Boolean,
        tilbakestillGrunnlag: () -> Unit,
    ) {
        oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = definisjon,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING -> {
                        val perioderBistandsvilkåretErRelevant = nårVurderingErRelevant(kontekst)

                        if (perioderBistandsvilkåretErRelevant.any { it.verdi } && kontekst.vurderingsbehovRelevanteForSteg.any { it in tvingerAvklaringsbehov }) {
                            return@oppdaterAvklaringsbehov true
                        }

                        val perioderBistandsvilkåretErVurdert = kontekst.forrigeBehandlingId
                            ?.let { forrigeBehandlingId ->
                                val forrigeBehandling = behandlingRepository.hent(forrigeBehandlingId)
                                val forrigeRettighetsperiode =
                                    /* Lagrer vi ned rettighetsperioden som ble brukt for en behandling noe sted? */
                                    vilkårsresultatRepository.hent(forrigeBehandlingId)
                                        .finnVilkår(Vilkårtype.ALDERSVILKÅRET)
                                        .tidslinje()
                                        .helePerioden()

                                nårVurderingErRelevant(
                                    kontekst.copy(
                                        /* TODO: hacky. Er faktisk bare behandlingId som brukes av sjekkene. */
                                        behandlingId = forrigeBehandlingId,
                                        forrigeBehandlingId = forrigeBehandling.forrigeBehandlingId,
                                        rettighetsperiode = forrigeRettighetsperiode,
                                        behandlingType = forrigeBehandling.typeBehandling(),
                                    )
                                )
                            }
                            ?: tidslinjeOf()

                        perioderBistandsvilkåretErRelevant.leftJoin(perioderBistandsvilkåretErVurdert) { erRelevant, erVurdert ->
                            erRelevant && erVurdert != true
                        }.any { it.verdi }
                    }

                    VurderingType.MELDEKORT -> false
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
                    VurderingType.IKKE_RELEVANT -> false
                }
            },
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )
    }
}
