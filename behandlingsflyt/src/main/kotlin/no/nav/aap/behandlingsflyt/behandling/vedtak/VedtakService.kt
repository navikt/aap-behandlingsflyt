package no.nav.aap.behandlingsflyt.behandling.vedtak

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vedtakRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    fun lagreVedtak(behandlingId: BehandlingId, vedtakstidspunkt: LocalDateTime, virkningstidspunkt: LocalDate?) {
        vedtakRepository.lagre(behandlingId, vedtakstidspunkt, virkningstidspunkt)
    }

    fun vedtakstidspunktFørstegangsbehandling(sakId: SakId): LocalDateTime? {
        val førstegangsbehandlingen = behandlingRepository.hentAlleFor(sakId)
            .firstOrNull { it.typeBehandling() == TypeBehandling.Førstegangsbehandling }
            ?: return null

        return vedtakRepository.hent(førstegangsbehandlingen.id)?.vedtakstidspunkt
    }

    fun vedtakstidspunkt(behandling: Behandling): LocalDateTime? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Klage -> {
                behandlingRepository.hentStegHistorikk(behandling.id)
                    .find { it.steg() == StegType.FATTE_VEDTAK && it.status() == StegStatus.AVSLUTTER }?.tidspunkt()
            }
            TypeBehandling.SvarFraAndreinstans -> null
            else -> vedtakRepository.hent(behandling.id)?.vedtakstidspunkt
        }
    }
}
