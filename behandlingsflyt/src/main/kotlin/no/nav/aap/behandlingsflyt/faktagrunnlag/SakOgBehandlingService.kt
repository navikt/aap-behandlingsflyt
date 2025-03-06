package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BehandlingTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BeriketBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class SakOgBehandlingService(
    private val grunnlagKopierer: GrunnlagKopierer,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository
) {

    fun finnEllerOpprettBehandling(key: Saksnummer, årsaker: List<Årsak>): BeriketBehandling {
        val sak = sakRepository.hent(key)

        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(sak.id)

        if (sisteBehandlingForSak == null) {
            return BeriketBehandling(
                behandling = behandlingRepository.opprettBehandling(
                    sakId = sak.id,
                    årsaker = årsaker,
                    typeBehandling = TypeBehandling.Førstegangsbehandling,
                    forrigeBehandlingId = null
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sakId = sak.id,
                    årsaker = årsaker,
                    typeBehandling = TypeBehandling.Revurdering,
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

    fun hentSakFor(behandlingId: BehandlingId): Sak {
        val behandling = behandlingRepository.hent(behandlingId)
        return sakRepository.hent(behandling.sakId)
    }

    fun oppdaterRettighetsperioden(sakId: SakId, brevkategori: InnsendingType, mottattDato: LocalDate) {
        if (setOf(InnsendingType.SØKNAD, InnsendingType.MELDEKORT).contains(brevkategori)) {
            val rettighetsperiode = sakRepository.hent(sakId).rettighetsperiode
            val fom = if (rettighetsperiode.fom.isAfter(mottattDato)) {
                mottattDato
            } else {
                rettighetsperiode.fom
            }
            val tom = if (mottattDato.plusYears(1).isAfter(rettighetsperiode.tom)) {
                mottattDato.plusYears(1)
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
}