package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.Opprettholdes
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.FASTSATT_PERIODE_PASSERT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MOTTATT_MELDEKORT
import no.nav.aap.lookup.repository.RepositoryProvider

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val resultatUtleder: ResultatUtleder,
    private val klageresultatUtleder: KlageresultatUtleder,
    private val vedtakRepository: VedtakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        resultatUtleder = ResultatUtleder(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        vedtakRepository = repositoryProvider.provide(),
    )

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov? {
        val behandling = behandlingRepository.hent(behandlingId)

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                val resultat = resultatUtleder.utledResultat(behandlingId)

                return when (resultat) {
                    Resultat.INNVILGELSE -> brevBehovInnvilgelse(behandling)
                    Resultat.AVSLAG -> Avslag
                    Resultat.TRUKKET -> null
                }
            }

            TypeBehandling.Revurdering -> {
                val vurderingsbehov = behandling.vurderingsbehov().map { it.type }.toSet()
                if (setOf(MOTTATT_MELDEKORT, FASTSATT_PERIODE_PASSERT).containsAll(vurderingsbehov)) {
                    return null
                }
                return VedtakEndring
            }

            TypeBehandling.Klage -> {
                val klageresulat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
                return when (klageresulat) {
                    is Avslått -> KlageAvvist
                    is Opprettholdes, is DelvisOmgjøres -> KlageOpprettholdelse
                    else -> null
                }
            }

            TypeBehandling.Tilbakekreving, TypeBehandling.SvarFraAndreinstans, TypeBehandling.OppfølgingsBehandling ->
                return null // TODO
        }
    }

    private fun brevBehovInnvilgelse(behandling: Behandling): BrevBehov {
        val vedtak = checkNotNull(vedtakRepository.hent(behandling.id)) {
            "Fant ikke vedtak for behandling med innvilgelse"
        }
        checkNotNull(vedtak.virkningstidspunkt) {
            "Vedtak for behandling med innvilgelse mangler virkningstidspunkt"
        }
        return Innvilgelse(vedtak.virkningstidspunkt)
    }
}
