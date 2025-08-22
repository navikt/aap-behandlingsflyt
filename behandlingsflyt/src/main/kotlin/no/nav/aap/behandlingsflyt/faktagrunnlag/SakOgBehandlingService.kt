package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SakOgBehandlingService(
    private val grunnlagKopierer: GrunnlagKopierer,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
    private val unleashGateway: UnleashGateway,
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
    ) : this(
        grunnlagKopierer = GrunnlagKopiererImpl(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    fun finnBehandling(behandlingReferanse: BehandlingReferanse): Behandling {
        return behandlingRepository.hent(behandlingReferanse)
    }

    /**
     * Ytelsesbehandling betyr førstegangsbehandling eller revurdering.
     */
    fun finnSisteYtelsesbehandlingFor(sakId: SakId): Behandling? {
        /* Finn siste ytelsesbehandling basert på `forrigeBehandlingId`-kjeden.
         * Behandlingene er i praksis en singly-linked list. Pekerne går "feil vei",
         * så vi regner ut bakover-pekerne.
        **/
        val ytelsesbehandlinger = behandlingRepository.hentAlleFor(
            sakId,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )
        val nesteId = mutableMapOf<BehandlingId, BehandlingId>()
        for (behandling in ytelsesbehandlinger) {
            if (behandling.forrigeBehandlingId != null) {
                nesteId[behandling.forrigeBehandlingId] = behandling.id
            }
        }

        var behandling = ytelsesbehandlinger.firstOrNull()?.id ?: return null

        while (nesteId[behandling] != null) {
            behandling = nesteId[behandling]!!
        }
        return ytelsesbehandlinger.find { it.id == behandling }
    }

    sealed interface OpprettetBehandling {
        val åpenBehandling: Behandling?
    }

    /* Dette er en vanlig, åpen behandling og behandlignen kan være åpen over tid, på tvers av
     * transaksjoner. */
    data class Ordinær(override val åpenBehandling: Behandling) : OpprettetBehandling

    /** Det er nå, potensielt, to åpne behandlinger. For at det ikke skal være observerbart, så
     * må denne nye behandlingen være i tilstanden IVERKSETTES eller AVSLUTTET om transaksjonen
     * commites. Hvis det ikke er mulig å avslutte behandlingen i transaksjonen,
     * så må transaksjonen avbrytes.
     *
     * Også hvis det ikke finnes en [åpenBehandling] så må [nyBehandling] avsluttes, ellers
     * vil senere hendelser kunne legge seg på feil behandling.
     *
     * @param nyBehandling Ny-opprettet behandling som må avsluttes (status: IVERKSETTES eller AVSLUTTET) i denne transaksjonen.
     *
     * @param åpenBehandling  Eksisterende åpen behandling.
     *
     * [forrigeBehandling][Behandling.forrigeBehandlingId] i [åpenBehandling] peker på [nyBehandling]. Men det er kaller sitt ansvar å
     * kopiere relevante opplysninger inn i [åpenBehandling] fra [nyBehandling].
     */
    data class MåBehandlesAtomært(
        val nyBehandling: Behandling,
        override val åpenBehandling: Behandling?,
    ) : OpprettetBehandling

    private val fasttrackKandidater = listOf(
        Vurderingsbehov.FRITAK_MELDEPLIKT,
        Vurderingsbehov.MOTTATT_MELDEKORT,
        Vurderingsbehov.FASTSATT_PERIODE_PASSERT
    )

    fun finnEllerOpprettBehandling(sakId: SakId, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak): OpprettetBehandling {
        val sisteYtelsesbehandling = finnSisteYtelsesbehandlingFor(sakId)
        val vurderingsbehov = vurderingsbehovOgÅrsak.vurderingsbehov
        val fasttrackkandidat = vurderingsbehov.isNotEmpty()
                && vurderingsbehov.all { it.type in fasttrackKandidater }
                && unleashGateway.isEnabled(BehandlingsflytFeature.FasttrackMeldekort)
        val mottokKabalHendelse = vurderingsbehov.any { it.type == Vurderingsbehov.MOTTATT_KABAL_HENDELSE }
        val mottokKlage = vurderingsbehov.any { it.type == Vurderingsbehov.MOTATT_KLAGE }

        val mottokOppfølgingsOppgave = vurderingsbehov.any { it.type == Vurderingsbehov.OPPFØLGINGSOPPGAVE }

        return when {
            mottokKlage -> Ordinær(opprettKlagebehandling(sisteYtelsesbehandling, vurderingsbehovOgÅrsak))
            mottokKabalHendelse -> Ordinær(opprettSvarFraKlageenhetBehandling(sisteYtelsesbehandling, vurderingsbehovOgÅrsak))
            mottokOppfølgingsOppgave -> Ordinær(opprettOppfølgingsbehandling(sisteYtelsesbehandling!!, vurderingsbehovOgÅrsak))

            /* Tilbakekreving kommer kanskje som et case her ... */

            sisteYtelsesbehandling == null ->
                Ordinær(opprettFørstegangsbehandling(sakId, vurderingsbehovOgÅrsak))

            sisteYtelsesbehandling.status().erAvsluttet() ->
                if (fasttrackkandidat)
                    MåBehandlesAtomært(opprettRevurdering(sisteYtelsesbehandling, vurderingsbehovOgÅrsak), null)
                else
                    Ordinær(opprettRevurdering(sisteYtelsesbehandling, vurderingsbehovOgÅrsak))


            sisteYtelsesbehandling.status().erÅpen() ->
                if (fasttrackkandidat && sisteYtelsesbehandling.typeBehandling() != TypeBehandling.Førstegangsbehandling)
                    MåBehandlesAtomært(
                        opprettRevurderingForranÅpenBehandling(sisteYtelsesbehandling, vurderingsbehovOgÅrsak),
                        sisteYtelsesbehandling
                    )
                else
                    Ordinær(oppdaterVurderingsbehovOgÅrsak(sisteYtelsesbehandling, vurderingsbehovOgÅrsak))

            else ->
                error("greier ikke å finne eller opprette behandling, uventet tilstand i saken")
        }
    }

    fun finnEllerOpprettOrdinærBehandling(sakId: SakId, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak): Behandling {
        return when (val b = finnEllerOpprettBehandling(sakId, vurderingsbehovOgÅrsak)) {
            is MåBehandlesAtomært -> error("skal ikke føre til atmoær behandling")
            is Ordinær -> b.åpenBehandling
        }
    }

    private fun opprettKlagebehandling(
        sisteYtelsesbehandling: Behandling?,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak
    ): Behandling {
        requireNotNull(sisteYtelsesbehandling) {
            "Mottok klage, men det finnes ingen eksisterende behandling"
        }

        //if referanse == null opprettBehandling
        return behandlingRepository.opprettBehandling(
            sakId = sisteYtelsesbehandling.sakId,
            typeBehandling = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
        )
        //else koble på eksisterende behandling?
    }

    private fun opprettOppfølgingsbehandling(sisteYtelsesbehandling: Behandling, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak): Behandling {
        requireNotNull(sisteYtelsesbehandling) {
            "Mottok oppfølgingsbehandling, men det finnes ingen eksisterende behandling. Behandling-ID: ${sisteYtelsesbehandling.id}"
        }

        return behandlingRepository.opprettBehandling(
            sisteYtelsesbehandling.sakId,
            typeBehandling = TypeBehandling.OppfølgingsBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
        )
    }

    private fun opprettSvarFraKlageenhetBehandling(
        sisteYtelsesbehandling: Behandling?,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak,
    ): Behandling {
        // Kan vurdere en bedre sjekk her, men vi validerer allerede ved mottak at en tilhørende klagebehandling eksisterer
        requireNotNull(sisteYtelsesbehandling) {
            "Mottok kabalehndelse, men det finnes ingen eksisterende behandling"
        }
        return behandlingRepository.opprettBehandling(
            sakId = sisteYtelsesbehandling.sakId,
            typeBehandling = TypeBehandling.SvarFraAndreinstans,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
        )
    }

    private fun opprettFørstegangsbehandling(
        sakId: SakId,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak,
    ): Behandling = behandlingRepository.opprettBehandling(
        sakId = sakId,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        forrigeBehandlingId = null,
        vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
    )

    private fun opprettRevurdering(
        sisteYtelsesbehandling: Behandling,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak,
    ): Behandling {
        check(!trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)) {
            "ikke lov å opprette ny behandling for trukket søknad ${sisteYtelsesbehandling.sakId}"
        }
        return behandlingRepository.opprettBehandling(
            sakId = sisteYtelsesbehandling.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = sisteYtelsesbehandling.id,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak
        ).also { behandling ->
            grunnlagKopierer.overfør(sisteYtelsesbehandling.id, behandling.id)
        }
    }

    private fun opprettRevurderingForranÅpenBehandling(
        apenRevurdering: Behandling,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak,
    ): Behandling {
        check(apenRevurdering.status().erÅpen())
        check(apenRevurdering.typeBehandling() == TypeBehandling.Revurdering)
        check(!trukketSøknadService.søknadErTrukket(apenRevurdering.id)) {
            "ikke lov å opprette ny behandling for trukket søknad ${apenRevurdering.sakId}"
        }

        val sisteAvsluttedeYtelsesvurdering = behandlingRepository.hent(apenRevurdering.forrigeBehandlingId!!)

        return behandlingRepository.opprettBehandling(
            sakId = apenRevurdering.sakId,
            typeBehandling = TypeBehandling.Revurdering,
            vurderingsbehovOgÅrsak = vurderingsbehovOgÅrsak,
            forrigeBehandlingId = sisteAvsluttedeYtelsesvurdering.id,
        ).also { behandling ->
            grunnlagKopierer.overfør(sisteAvsluttedeYtelsesvurdering.id, behandling.id)
            behandlingRepository.flyttForrigeBehandlingId(apenRevurdering.id, behandling.id)
        }
    }

    private fun oppdaterVurderingsbehovOgÅrsak(
        sisteYtelsesbehandling: Behandling,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak
    ): Behandling {
        check(!trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)) {
            "ikke lov å oppdatere behandling for trukket søknad ${sisteYtelsesbehandling.sakId}"
        }
        // Valider at behandlingen står i et sted hvor den kan data
        validerStegStatus(sisteYtelsesbehandling)
        behandlingRepository.oppdaterVurderingsbehovOgÅrsak(sisteYtelsesbehandling, vurderingsbehovOgÅrsak)
        return sisteYtelsesbehandling
    }

    fun finnEllerOpprettOrdinærBehandling(saksnummer: Saksnummer, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak): Behandling {
        val sak = sakRepository.hent(saksnummer)

        return finnEllerOpprettOrdinærBehandling(sak.id, vurderingsbehovOgÅrsak)
    }

    fun finnEllerOpprettBehandling(saksnummer: Saksnummer, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak): OpprettetBehandling {
        val sak = sakRepository.hent(saksnummer)

        return finnEllerOpprettBehandling(sak.id, vurderingsbehovOgÅrsak)
    }

    fun lukkBehandling(behandlingId: BehandlingId) {
        // Valider siste stegstatus behandlingen
        validerAtSisteStegstatusErAvsluttet(behandlingId)

        behandlingRepository.oppdaterBehandlingStatus(
            behandlingId = behandlingId,
            status = Status.AVSLUTTET
        )
    }

    private fun validerAtSisteStegstatusErAvsluttet(behandlingId: BehandlingId) {
        val oppdatertBehandling = behandlingRepository.hent(behandlingId)
        val sisteSteg = oppdatertBehandling.aktivtStegTilstand()
        require(sisteSteg.status() == StegStatus.AVSLUTTER)
    }

    fun hentSakFor(behandlingId: BehandlingId): Sak {
        val behandling = behandlingRepository.hent(behandlingId)
        return sakRepository.hent(behandling.sakId)
    }

    fun oppdaterRettighetsperioden(sakId: SakId, brevkategori: InnsendingType, mottattDato: LocalDate) {
        if (brevkategori == InnsendingType.SØKNAD) {
            val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode
            val fom = if (rettighetsperiode.fom.isAfter(mottattDato)) {
                mottattDato
            } else {
                rettighetsperiode.fom
            }
            val tom = if (mottattDato.plusYears(1).minusDays(1).isAfter(rettighetsperiode.tom)) {
                mottattDato.plusYears(1).minusDays(1)
            } else {
                rettighetsperiode.tom
            }
            val periode = Periode(
                fom,
                tom
            ) // TODO: Usikker på om dette blir helt korrekt.. // Spør Peter
            if (periode != rettighetsperiode) {
                sakRepository.oppdaterRettighetsperiode(sakId, periode)
            }
        }
    }

    fun oppdaterVurderingsbehovTilBehandling(behandling: Behandling, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak) {
        behandlingRepository.oppdaterVurderingsbehovOgÅrsak(behandling, vurderingsbehovOgÅrsak)
    }

    fun overstyrRettighetsperioden(sakId: SakId, startDato: LocalDate, sluttDato: LocalDate) {
        val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode
        val periode = Periode(
            startDato,
            sluttDato
        )
        if (periode != rettighetsperiode) {
            sakRepository.oppdaterRettighetsperiode(sakId, periode)
        }
    }


    private fun validerStegStatus(behandling: Behandling) {
        val flyt = behandling.flyt()
        if (!flyt.skalOppdatereFaktagrunnlag()) {
            throw IllegalStateException("Behandlingen[${behandling.referanse}] kan ikke motta opplysinger nå, avventer fullføring av steg som ligger etter at oppdatering av faktagrunnlag opphører.")
        }
    }
}
