package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklaringsbehovService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avbrytRevurderingService: AvbrytRevurderingService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)
    )

    @Deprecated("Oppdater avklaringsbehov med de andre metodene i AvklaringsbehovService")
    fun avbrytForSteg(behandlingId: BehandlingId, steg: StegType) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.avbrytForSteg(steg)
    }

    fun oppdaterAvklaringsbehov(
        avklaringsbehovene: Avklaringsbehovene,
        definisjon: Definisjon,
        vedtakBehøverVurdering: () -> Boolean,
        erTilstrekkeligVurdert: () -> Boolean,
        tilbakestillGrunnlag: () -> Unit,
        kontekst: FlytKontekstMedPerioder
    ) {
        oppdaterAvklaringsbehov(
            avklaringsbehovene,
            definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            perioderSomIkkeErTilstrekkeligVurdert = { null },
            perioderVedtaketBehøverVurdering = { null },
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

    }

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
    private fun oppdaterAvklaringsbehov(
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
        perioderVedtaketBehøverVurdering: () -> Set<Periode>?,

        /** Er avklaringsbehovet [definisjon] tilstrekkelig vurdert for å fortsette behandlingen?
         *
         * Denne funksjonen kalles kun om `vedtakBehøverVurdering() == true` og avklaringsbehovet
         * [definisjon] allerede har en løsning. Merk at selv om definisjonen allerede har en løsning,
         * så kan den løsningen ha blitt rullet tilbake (se [tilbakestillGrunnlag]).
         */
        perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode>?,
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

        // TODO: Fjern denne når alle kall tar i bruk perioderSomIkkeErTilstrekkeligVurdert
        val erTilstrekkeligVurdertBakoverkompatibel =
            { erTilstrekkeligVurdert() || perioderSomIkkeErTilstrekkeligVurdert()?.isEmpty() == true }

        if (vedtakBehøverVurdering()) {
            if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken() || avklaringsbehov.status() == AVBRUTT) {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov?.status()) {
                    OPPRETTET -> {
                        /* ønsket tilstand er OPPRETTET */
                        avklaringsbehovene.oppdaterPerioder(
                            avklaringsbehov.definisjon,
                            perioderSomIkkeErTilstrekkeligVurdert(),
                            perioderVedtaketBehøverVurdering()
                        )
                    }

                    null, AVBRUTT ->
                        avklaringsbehovene.leggTil(
                            listOf(definisjon),
                            definisjon.løsesISteg,
                            perioderSomIkkeErTilstrekkeligVurdert()
                        )

                    TOTRINNS_VURDERT,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    AVSLUTTET ->
                        error("Ikke mulig: fikk ${avklaringsbehov.status()}")
                }
            } else if (erTilstrekkeligVurdertBakoverkompatibel()) {
                /* ønsket tilstand: ... */
                when (avklaringsbehov.status()) {
                    OPPRETTET, AVBRUTT ->
                        avklaringsbehovene.internalAvslutt(definisjon)

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
                        avklaringsbehovene.oppdaterPerioder(
                            avklaringsbehov.definisjon,
                            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert(),
                            perioderVedtaketBehøverVurdering()
                        )

                    }

                    AVSLUTTET,
                    TOTRINNS_VURDERT,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    AVBRUTT -> {
                        avklaringsbehovene.leggTil(
                            listOf(definisjon),
                            definisjon.løsesISteg,
                            perioderSomIkkeErTilstrekkeligVurdert(),
                            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering()
                        )
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
                    avklaringsbehovene.internalAvbryt(definisjon)
                    if (!avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)) {
                        tilbakestillGrunnlag()
                    }
                }
            }
        }
    }

    private fun oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
        avklaringsbehovene: Avklaringsbehovene,
        behandlingRepository: BehandlingRepository,
        vilkårsresultatRepository: VilkårsresultatRepository,
        definisjon: Definisjon,
        tvingerAvklaringsbehov: Set<Vurderingsbehov>,
        nårVurderingErRelevant: (kontekst: FlytKontekstMedPerioder) -> Tidslinje<Boolean>,
        kontekst: FlytKontekstMedPerioder,
        perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode>?,
        nårVurderingErGyldig: () -> Tidslinje<Boolean>?,
        tilbakestillGrunnlag: () -> Unit,
    ) {
        val (behøverVurdering, perioderVedtaketBehøverVurdering) = when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                val perioderVilkåretErRelevant = nårVurderingErRelevant(kontekst)

                if (perioderVilkåretErRelevant.segmenter()
                        .any { it.verdi } && kontekst.vurderingsbehovRelevanteForSteg.any { it in tvingerAvklaringsbehov }
                ) {
                    Pair(
                        true, emptySet() // Vi behøver vurdering, men har ingen obligatoriske perioder
                    )
                } else {

                    val perioderVilkåretErVurdert = kontekst.forrigeBehandlingId
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
                        .orEmpty()

                    val perioderSomBehøverVurdering =
                        perioderVilkåretErRelevant.leftJoin(perioderVilkåretErVurdert) { erRelevant, erVurdert ->
                            erRelevant && erVurdert != true
                        }.filter { it.verdi }.komprimer().perioder().toSet()
                    Pair(perioderSomBehøverVurdering.isNotEmpty(), perioderSomBehøverVurdering)
                }
            }

            VurderingType.MELDEKORT -> Pair(false, emptySet())
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> Pair(false, emptySet())
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> Pair(false, emptySet())
            VurderingType.IKKE_RELEVANT -> Pair(false, emptySet())
        }

        oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = definisjon,
            vedtakBehøverVurdering = { behøverVurdering },
            perioderVedtaketBehøverVurdering = { perioderVedtaketBehøverVurdering },
            perioderSomIkkeErTilstrekkeligVurdert =
                {
                    if (perioderSomIkkeErTilstrekkeligVurdert() != null) {
                        perioderSomIkkeErTilstrekkeligVurdert()!!.toSet()
                    } else {
                        if (nårVurderingErGyldig() == null) {
                            null
                        } else {
                            nårVurderingErRelevant(kontekst).leftJoin(nårVurderingErGyldig()!!) { erRelevant, erGyldig ->
                                !erRelevant || erGyldig == true
                            }.komprimer().filter { !it.verdi }.perioder().toSet()
                        }
                    }
                },
            erTilstrekkeligVurdert =
                { false },
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )
    }

    @Deprecated("Bruk nårVurderingErGyldig i stedet for perioderSomIkkeErTilstrekkeligVurdert")
    fun oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårGammel(
        avklaringsbehovene: Avklaringsbehovene,
        behandlingRepository: BehandlingRepository,
        vilkårsresultatRepository: VilkårsresultatRepository,
        definisjon: Definisjon,
        tvingerAvklaringsbehov: Set<Vurderingsbehov>,
        nårVurderingErRelevant: (kontekst: FlytKontekstMedPerioder) -> Tidslinje<Boolean>,
        kontekst: FlytKontekstMedPerioder,
        perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode>?,
        tilbakestillGrunnlag: () -> Unit
    ) {
        return oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene,
            behandlingRepository,
            vilkårsresultatRepository,
            definisjon,
            tvingerAvklaringsbehov,
            nårVurderingErRelevant,
            kontekst,
            perioderSomIkkeErTilstrekkeligVurdert,
            { null },
            tilbakestillGrunnlag
        )
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
        /**
         * Hvilke perioder vurdering er relevant.
         * Brukes til å utlede hvorvidt vedtaket behøver vurdering.
         */
        nårVurderingErRelevant: (kontekst: FlytKontekstMedPerioder) -> Tidslinje<Boolean>,
        /**
         * Hvilke perioder behandlingen har en god nok vurdering for.
         * Det vil løftes avklaringsbehov for relevante perioder som mangler gyldig vurdering.
         */
        nårVurderingErGyldig: () -> Tidslinje<Boolean>,
        kontekst: FlytKontekstMedPerioder,
        tilbakestillGrunnlag: () -> Unit
    ) {
        return oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            avklaringsbehovene,
            behandlingRepository,
            vilkårsresultatRepository,
            definisjon,
            tvingerAvklaringsbehov,
            nårVurderingErRelevant,
            kontekst,
            { null },
            nårVurderingErGyldig,
            tilbakestillGrunnlag

        )
    }
}