package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.DelvisOmgjøres
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Opprettholdes
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling.MOTTATT_MELDEKORT
import no.nav.aap.lookup.repository.RepositoryProvider

class BrevUtlederService(
    private val behandlingRepository: BehandlingRepository,
    private val resultatUtleder: ResultatUtleder,
    private val klageresultatUtleder: KlageresultatUtleder,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        resultatUtleder = ResultatUtleder(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
    )

    fun utledBehovForMeldingOmVedtak(behandlingId: BehandlingId): BrevBehov {
        val behandling = behandlingRepository.hent(behandlingId)

        when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                val resultat = resultatUtleder.utledResultat(behandlingId)

                return when (resultat) {
                    Resultat.INNVILGELSE -> BrevBehov(TypeBrev.VEDTAK_INNVILGELSE)
                    Resultat.AVSLAG -> BrevBehov(TypeBrev.VEDTAK_AVSLAG)
                    Resultat.TRUKKET -> BrevBehov(null)
                }
            }

            TypeBehandling.Revurdering -> {
                val årsakerTilBehandling = behandling.årsaker().map { it.type }.toSet()
                if (setOf(MOTTATT_MELDEKORT, FASTSATT_PERIODE_PASSERT).containsAll(årsakerTilBehandling)) {
                    return BrevBehov(null)
                }
                return BrevBehov(TypeBrev.VEDTAK_ENDRING)
            }

            TypeBehandling.Klage -> {
                val klageresulat = klageresultatUtleder.utledKlagebehandlingResultat(behandlingId)
                return when (klageresulat) {
                    is Avslått -> BrevBehov(TypeBrev.KLAGE_AVVIST)
                    is Opprettholdes, is DelvisOmgjøres -> BrevBehov(TypeBrev.KLAGE_OPPRETTHOLDELSE)
                    else -> BrevBehov(null)
                }
            }

            TypeBehandling.Tilbakekreving, TypeBehandling.SvarFraAndreinstans ->
                return BrevBehov(null) // TODO
        }
    }
}
