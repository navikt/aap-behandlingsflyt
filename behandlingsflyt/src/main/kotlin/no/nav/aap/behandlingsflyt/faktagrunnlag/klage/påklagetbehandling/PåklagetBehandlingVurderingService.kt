package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDate

class PåklagetBehandlingVurderingService(
    val behandlingRepository: BehandlingRepository,
    val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    val vedtakService: VedtakService
) {
    fun hentGjeldendeVurderingMedReferanse(behandlingsreferanse: BehandlingReferanse): PåklagetBehandlingVurderingMedReferanse? {
        return påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandlingsreferanse)
    }

    fun hentAlleBehandlingerMedVedtakForPerson(personId: PersonId): List<BehandlingMedVedtak> {
        return behandlingRepository.hentAlleMedVedtakFor(personId, TypeBehandling.entries)
    }

    fun hentAlleKlagerMedVedaksdato(sakId: SakId): List<KlagebehandlingMedVedtaksdato> {
        val klagebehandlinger = behandlingRepository.hentAlleFor(sakId, listOf(TypeBehandling.Klage))
        return klagebehandlinger.mapNotNull { behandling ->
            vedtakService.vedtakstidspunkt(behandling)?.toLocalDate()?.let { vedtaksdato ->
                KlagebehandlingMedVedtaksdato(
                    behandling = behandling,
                    vedtaksdato = vedtaksdato
                )
            }
        }
    }
}

data class KlagebehandlingMedVedtaksdato(
    val behandling: Behandling,
    val vedtaksdato: LocalDate
)