package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val underveisRepository: UnderveisRepository,
) {
    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov {
        val behandling = behandlingRepository.hent(behandlingId)

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                val underveisGrunnlag = underveisRepository.hent(behandlingId)
                val oppfyltePerioder = underveisGrunnlag.perioder.filter { it.utfall == Utfall.OPPFYLT }

                return if (oppfyltePerioder.isNotEmpty()) {
                    BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
                } else {
                    BrevBehov(TypeBrev.VEDTAK_AVSLAG)
                }
            }

            TypeBehandling.Revurdering -> {
                val årsakerTilBehandling = behandling.årsaker().map { it.type }.distinct()
                if (årsakerTilBehandling.size == 1 &&
                    årsakerTilBehandling.contains(ÅrsakTilBehandling.MOTTATT_MELDEKORT)
                ) {
                    return BrevBehov(null)
                }
                return BrevBehov(TypeBrev.VEDTAK_ENDRING)
            }

            TypeBehandling.Tilbakekreving, TypeBehandling.Klage ->
                return BrevBehov(null) // TODO
        }
    }
}
