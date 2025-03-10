package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.flyt.utledType
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
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
                    typeBehandling = utledBehandlingstype(sisteBehandlingForSak, årsaker),
                    forrigeBehandlingId = null
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sakId = sak.id,
                    årsaker = årsaker,
                    typeBehandling = utledBehandlingstype(sisteBehandlingForSak, årsaker),
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
                if (årsaker.any { it.type == ÅrsakTilBehandling.MOTATT_KLAGE }) TODO("Hva skal skje med klage mottatt for åpen behandling?")
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

    private fun utledBehandlingstype(sisteBeahndlingForSak: Behandling?, årsaker: List<Årsak>): TypeBehandling {
        if (årsaker.any { it.type == ÅrsakTilBehandling.MOTATT_KLAGE }) {
            return when (sisteBeahndlingForSak) {
                null -> TODO("Hva skal skje når man har mottatt klage men det ikke finnes en behandling for saken?")
                else -> TypeBehandling.Klage
            }
        } else {
            return when (sisteBeahndlingForSak) {
                null -> TypeBehandling.Førstegangsbehandling
                else -> TypeBehandling.Revurdering
            }
        }
    }

    private fun validerStegStatus(behandling: Behandling) {
        val flyt = utledType(behandling.typeBehandling()).flyt()
        // TODO Utvide med regler for hva som kan knyttes til en behandling og når den eventuelt skal tilbake likevel
        // Om den skal tilbake krever det endringer for å ta hensyn til disse
        if (!flyt.skalOppdatereFaktagrunnlag()) {
            throw IllegalStateException("Behandlingen[${behandling.referanse}] kan ikke motta opplysinger nå, avventer fullføring av steg som ligger etter at oppdatering av faktagrunnlag opphører.")
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