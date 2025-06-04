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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BehandlingTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BeriketBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class SakOgBehandlingService(
    private val grunnlagKopierer: GrunnlagKopierer,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val trukketSøknadService: TrukketSøknadService,
) {
    constructor(repositoryProvider: RepositoryProvider): this(
        grunnlagKopierer = GrunnlagKopiererImpl(repositoryProvider),
        sakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        trukketSøknadService = TrukketSøknadService(repositoryProvider),
    )

    fun finnBehandling(behandlingReferanse: BehandlingReferanse): Behandling {
        return behandlingRepository.hent(behandlingReferanse)
    }

    fun finnEllerOpprettBehandling(sakId: SakId, årsaker: List<Årsak>): BeriketBehandling {
        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(
            sakId,
            listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)
        )

        val behandlingstype = utledBehandlingstype(sisteBehandlingForSak, årsaker)

        if (sisteBehandlingForSak != null && behandlingstype in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering)) {
            check(!trukketSøknadService.søknadErTrukket(sisteBehandlingForSak.id)) {
                "ikke lov å opprette ny behandling for trukket søknad $sakId"
            }
        }

        if (behandlingstype == TypeBehandling.Klage) {
            // TODO: Se på om vi må knytte klagebehandling mot en gitt behandling
            return BeriketBehandling(
                behandling = behandlingRepository.opprettBehandling(
                    sakId = sakId,
                    årsaker = årsaker,
                    typeBehandling = behandlingstype,
                    forrigeBehandlingId = null
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )
        } else if (sisteBehandlingForSak == null) {
            return BeriketBehandling(
                behandling = behandlingRepository.opprettBehandling(
                    sakId = sakId,
                    årsaker = årsaker,
                    typeBehandling = behandlingstype,
                    forrigeBehandlingId = null
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sakId = sakId,
                    årsaker = årsaker,
                    typeBehandling = behandlingstype,
                    forrigeBehandlingId = sisteBehandlingForSak.id
                )

                val beriketBehandling = BeriketBehandling(
                    behandling = nyBehandling,
                    tilstand = BehandlingTilstand.NY,
                    sisteAvsluttedeBehandling = sisteBehandlingForSak.id
                )
                if (beriketBehandling.skalKopierFraSisteBehandling()) {
                    grunnlagKopierer.overfør(
                        requireNotNull(beriketBehandling.sisteAvsluttedeBehandling),
                        nyBehandling.id
                    )
                }

                return beriketBehandling

            } else {
                // Valider at behandlingen står i et sted hvor den kan data
                validerStegStatus(sisteBehandlingForSak)
                // Oppdater årsaker hvis nødvendig
                behandlingRepository.oppdaterÅrsaker(sisteBehandlingForSak, årsaker)
                return BeriketBehandling(
                    behandling = sisteBehandlingForSak,
                    tilstand = BehandlingTilstand.EKSISTERENDE,
                    sisteAvsluttedeBehandling = null
                )
            }
        }
    }

    fun finnEllerOpprettBehandling(saksnummer: Saksnummer, årsaker: List<Årsak>): BeriketBehandling {
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

    private fun utledBehandlingstype(sisteBehandlingForSak: Behandling?, årsaker: List<Årsak>): TypeBehandling {
        return if (årsaker.any { it.type == ÅrsakTilBehandling.MOTATT_KLAGE }) {
            when (sisteBehandlingForSak) {
                null -> throw IllegalArgumentException("Mottok klage, men det finnes ingen eksisterende behandling")
                else -> TypeBehandling.Klage
            }
        } else {
            when (sisteBehandlingForSak) {
                null -> TypeBehandling.Førstegangsbehandling
                else -> TypeBehandling.Revurdering
            }
        }
    }
}
