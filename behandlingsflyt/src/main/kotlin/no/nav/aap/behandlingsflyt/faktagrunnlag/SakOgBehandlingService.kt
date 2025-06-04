package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
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
    constructor(repositoryProvider: RepositoryProvider, unleashGateway: UnleashGateway = GatewayProvider.provide()) : this(
        grunnlagKopierer = GrunnlagKopiererImpl(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
        unleashGateway = unleashGateway,
    )

    fun finnBehandling(behandlingReferanse: BehandlingReferanse): Behandling {
        return behandlingRepository.hent(behandlingReferanse)
    }

    fun finnSisteYtelsesbehandlingFor(sakId: SakId): Behandling? {
        return behandlingRepository.finnSisteBehandlingFor(
            sakId,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )
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
        ÅrsakTilBehandling.FRITAK_MELDEPLIKT,
        ÅrsakTilBehandling.MOTTATT_MELDEKORT,
        ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT
    )

    fun finnEllerOpprettBehandlingFasttrack(sakId: SakId, årsaker: List<Årsak>): OpprettetBehandling {
        val sisteYtelsesbehandling = finnSisteYtelsesbehandlingFor(sakId)
        val fasttrackkandidat = årsaker.isNotEmpty()
                && årsaker.all { it.type in fasttrackKandidater }
                && unleashGateway.isEnabled(BehandlingsflytFeature.FasttrackMeldekort)

        return when {
            årsaker.any { it.type == ÅrsakTilBehandling.MOTATT_KLAGE } ->
                Ordinær(opprettKlagebehandling(sisteYtelsesbehandling, sakId, årsaker))

            /* Tilbakekreving kommer kanskje som et case her ... */

            sisteYtelsesbehandling == null ->
                Ordinær(opprettFørstegangsbehandling(sakId, årsaker))

            sisteYtelsesbehandling.status().erAvsluttet() ->
                if (fasttrackkandidat)
                    MåBehandlesAtomært(opprettRevurdering(sisteYtelsesbehandling, årsaker), null)
                else
                    Ordinær(opprettRevurdering(sisteYtelsesbehandling, årsaker))


            sisteYtelsesbehandling.status().erÅpen() ->
                if (fasttrackkandidat && sisteYtelsesbehandling.typeBehandling() != TypeBehandling.Førstegangsbehandling)
                    MåBehandlesAtomært(opprettRevurderingForranÅpenBehandling(sisteYtelsesbehandling, årsaker), sisteYtelsesbehandling)
                else
                    Ordinær(oppdaterÅrsaker(sisteYtelsesbehandling, årsaker))

            else ->
                error("greier ikke å finne eller opprette behandling, uventet tilstand i saken")
        }
    }

    fun finnEllerOpprettBehandling(sakId: SakId, årsaker: List<Årsak>): Behandling {
        return when (val b = finnEllerOpprettBehandlingFasttrack(sakId, årsaker)) {
            is MåBehandlesAtomært -> error("skal ikke føre til atmoær behandling")
            is Ordinær -> b.åpenBehandling
        }
    }

    private fun opprettKlagebehandling(
        sisteYtelsesbehandling: Behandling?,
        sakId: SakId,
        årsaker: List<Årsak>
    ): Behandling {
        requireNotNull(sisteYtelsesbehandling) {
            "Mottok klage, men det finnes ingen eksisterende behandling"
        }
        return behandlingRepository.opprettBehandling(
            sakId = sakId,
            årsaker = årsaker,
            typeBehandling = TypeBehandling.Klage,
            forrigeBehandlingId = null,
        )
    }

    private fun opprettFørstegangsbehandling(
        sakId: SakId,
        årsaker: List<Årsak>
    ): Behandling = behandlingRepository.opprettBehandling(
        sakId = sakId,
        årsaker = årsaker,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        forrigeBehandlingId = null,
    )

    private fun opprettRevurdering(
        sisteYtelsesbehandling: Behandling,
        årsaker: List<Årsak>
    ): Behandling {
        check(!trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)) {
            "ikke lov å opprette ny behandling for trukket søknad ${sisteYtelsesbehandling.sakId}"
        }
        return behandlingRepository.opprettBehandling(
            sakId = sisteYtelsesbehandling.sakId,
            årsaker = årsaker,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = sisteYtelsesbehandling.id,
        ).also { behandling ->
            grunnlagKopierer.overfør(sisteYtelsesbehandling.id, behandling.id)
        }
    }

    private fun opprettRevurderingForranÅpenBehandling(
        apenRevurdering: Behandling,
        årsaker: List<Årsak>
    ): Behandling {
        check(apenRevurdering.status().erÅpen())
        check(apenRevurdering.typeBehandling() == TypeBehandling.Revurdering)
        check(!trukketSøknadService.søknadErTrukket(apenRevurdering.id)) {
            "ikke lov å opprette ny behandling for trukket søknad ${apenRevurdering.sakId}"
        }

        val sisteAvsluttedeYtelsesvurdering = behandlingRepository.hent(apenRevurdering.forrigeBehandlingId!!)

        return behandlingRepository.opprettBehandling(
            sakId = apenRevurdering.sakId,
            årsaker = årsaker,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = sisteAvsluttedeYtelsesvurdering.id,
        ).also { behandling ->
            grunnlagKopierer.overfør(sisteAvsluttedeYtelsesvurdering.id, behandling.id)
            behandlingRepository.flyttForrigeBehandlingId(apenRevurdering.id, behandling.id)
        }
    }

    private fun oppdaterÅrsaker(
        sisteYtelsesbehandling: Behandling,
        årsaker: List<Årsak>
    ): Behandling {
        check(!trukketSøknadService.søknadErTrukket(sisteYtelsesbehandling.id)) {
            "ikke lov å oppdatere behandling for trukket søknad ${sisteYtelsesbehandling.sakId}"
        }
        // Valider at behandlingen står i et sted hvor den kan data
        validerStegStatus(sisteYtelsesbehandling)
        behandlingRepository.oppdaterÅrsaker(sisteYtelsesbehandling, årsaker)
        return sisteYtelsesbehandling
    }

    fun finnEllerOpprettBehandling(saksnummer: Saksnummer, årsaker: List<Årsak>): Behandling {
        val sak = sakRepository.hent(saksnummer)

        return finnEllerOpprettBehandling(sak.id, årsaker)
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
            ) // TODO: Usikker på om dette blir helt korrekt..
            if (periode != rettighetsperiode) {
                sakRepository.oppdaterRettighetsperiode(sakId, periode)
            }
        }
    }

    fun oppdaterÅrsakerTilBehandling(behandling: Behandling, nyeÅrsaker: List<Årsak>) {
        behandlingRepository.oppdaterÅrsaker(behandling, nyeÅrsaker)
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
        // TODO Utvide med regler for hva som kan knyttes til en behandling og når den eventuelt skal tilbake likevel
        // Om den skal tilbake krever det endringer for å ta hensyn til disse
        if (!flyt.skalOppdatereFaktagrunnlag()) {
            throw IllegalStateException("Behandlingen[${behandling.referanse}] kan ikke motta opplysinger nå, avventer fullføring av steg som ligger etter at oppdatering av faktagrunnlag opphører.")
        }
    }
}
