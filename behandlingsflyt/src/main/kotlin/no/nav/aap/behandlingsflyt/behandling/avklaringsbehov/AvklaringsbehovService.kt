package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
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
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class AvklaringsbehovService(
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val trukketSøknadService: TrukketSøknadService
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider)
    )

    fun oppdaterAvklaringsbehov(
        definisjon: Definisjon,
        vedtakBehøverVurdering: () -> Boolean,
        erTilstrekkeligVurdert: () -> Boolean,
        tilbakestillGrunnlag: () -> Unit,
        kontekst: FlytKontekstMedPerioder
    ) {
        oppdaterAvklaringsbehov(
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
         *
         * @return `null` betyr at [erTilstrekkeligVurdert] benyttes i stedet for periodebasert sjekk.
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
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

        // TODO: Fjern denne når alle kall tar i bruk perioderSomIkkeErTilstrekkeligVurdert
        val erTilstrekkeligVurdertBakoverkompatibel =
            { erTilstrekkeligVurdert() || perioderSomIkkeErTilstrekkeligVurdert()?.isEmpty() == true }

        if (vedtakBehøverVurdering()) {
            avbrytAvklaringsbehovOmVurderingsbehovErNyere(kontekst, avklaringsbehov, avklaringsbehovene, definisjon)
            if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken() || avklaringsbehov.status() == AVBRUTT) {
                /* ønsket tilstand: OPPRETTET */
                when (avklaringsbehov?.status()) {
                    OPPRETTET -> {
                        /* ønsket tilstand er OPPRETTET */
                        avklaringsbehovene.oppdaterPerioder(
                            avklaringsbehov.definisjon,
                            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert(),
                            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering()
                        )
                    }

                    null, AVBRUTT ->
                        avklaringsbehovene.leggTil(
                            definisjon,
                            definisjon.løsesISteg,
                            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert(),
                            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering()
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
                            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering()
                        )

                    }

                    AVSLUTTET,
                    TOTRINNS_VURDERT,
                    SENDT_TILBAKE_FRA_BESLUTTER,
                    KVALITETSSIKRET,
                    SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                    AVBRUTT -> {
                        avklaringsbehovene.leggTil(
                            definisjon,
                            definisjon.løsesISteg,
                            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert(),
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
                    val erFrivilligAvklaringsbehov = avklaringsbehov.definisjon.erFrivillig()

                    val søknadErIkkeTrukket = !trukketSøknadService.søknadErTrukket(kontekst.behandlingId)
                    if (erFrivilligAvklaringsbehov && søknadErIkkeTrukket) {
                        return
                    }

                    avklaringsbehovene.internalAvbryt(definisjon)
                    if (!avbrytRevurderingService.revurderingErAvbrutt(kontekst.behandlingId)) {
                        tilbakestillGrunnlag()
                    }
                }
            }
        }
    }

    private fun avbrytAvklaringsbehovOmVurderingsbehovErNyere(
        kontekst: FlytKontekstMedPerioder,
        avklaringsbehov: Avklaringsbehov?,
        avklaringsbehovene: Avklaringsbehovene,
        definisjon: Definisjon
    ) {
        val nyesteVurderingsbehov = kontekst.vurderingsbehovRelevanteForStegMedPerioder.maxOfOrNull { it.oppdatertTid }
        val nyesteAvklaringsbehovEndring = avklaringsbehov?.aktivHistorikk?.maxOfOrNull { it.tidsstempel } ?: return
        val vurderingsbehovErNyere = nyesteVurderingsbehov != null && nyesteVurderingsbehov.isAfter(
            nyesteAvklaringsbehovEndring
        )
        if (vurderingsbehovErNyere) {
            avklaringsbehovene.internalAvbryt(definisjon)
        }
    }

    private fun oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
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

                if (perioderVilkåretErRelevant.segmenter().any { it.verdi }
                    && kontekst.vurderingsbehovRelevanteForSteg.any { it in tvingerAvklaringsbehov }
                ) {
                    // Vi behøver vurdering, men har ikke nødvendigvis noen obligatoriske perioder
                    Pair(true, perioderSomBehøverVurdering)
                } else {
                    Pair(perioderSomBehøverVurdering.isNotEmpty(), perioderSomBehøverVurdering)
                }
            }

            VurderingType.MELDEKORT -> Pair(false, emptySet())
            VurderingType.AUTOMATISK_BREV -> Pair(false, emptySet())
            VurderingType.UTVID_VEDTAKSLENGDE -> Pair(false, emptySet())
            VurderingType.MIGRER_RETTIGHETSPERIODE -> Pair(false, emptySet())
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> Pair(false, emptySet())
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> Pair(false, emptySet())
            VurderingType.IKKE_RELEVANT -> Pair(false, emptySet())
        }

        oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = { behøverVurdering },
            perioderVedtaketBehøverVurdering = { perioderVedtaketBehøverVurdering },
            perioderSomIkkeErTilstrekkeligVurdert =
                {
                    val perioderSomIkkeErTilstrekkeligVurdertEvaluert = perioderSomIkkeErTilstrekkeligVurdert()
                    if (perioderSomIkkeErTilstrekkeligVurdertEvaluert != null) {
                        perioderSomIkkeErTilstrekkeligVurdertEvaluert.toSet()
                    } else {
                        val nårVurderingErGyldigTidslinje = nårVurderingErGyldig()
                        if (nårVurderingErGyldigTidslinje == null) {
                            null
                        } else {
                            nårVurderingErRelevant(kontekst).leftJoin(nårVurderingErGyldigTidslinje) { erRelevant, erGyldig ->
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

    /**
     * Her sender man inn eksplistte perioderSomIkkeErTilstrekkeligVurdert.
     * Dette kan være nyttig dersom man må se på perioder som befinner seg utenfor perioder som behøver vurdering;
     * for eksempel hvis man ikke skal tillate vurderinger utenfor nårVurderingErRelevant
     */
    fun oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
        definisjon: Definisjon,
        tvingerAvklaringsbehov: Set<Vurderingsbehov>,
        nårVurderingErRelevant: (kontekst: FlytKontekstMedPerioder) -> Tidslinje<Boolean>,
        kontekst: FlytKontekstMedPerioder,
        perioderSomIkkeErTilstrekkeligVurdert: () -> Set<Periode>?,
        tilbakestillGrunnlag: () -> Unit
    ) {
        return oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon,
            tvingerAvklaringsbehov,
            nårVurderingErRelevant,
            kontekst,
            perioderSomIkkeErTilstrekkeligVurdert,
            { null },
            tilbakestillGrunnlag
        )
    }

    /** Spesialtilfelle av [oppdaterAvklaringsbehov] for vilkår som er periodisert. Brukeren
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
        oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
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
