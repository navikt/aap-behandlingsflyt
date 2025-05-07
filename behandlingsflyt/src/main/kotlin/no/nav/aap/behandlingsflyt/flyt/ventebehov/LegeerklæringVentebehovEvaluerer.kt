package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider

class LegeerklæringVentebehovEvaluerer(
    private val mottattDokumentRepository: MottattDokumentRepository,
): SpesifikkVentebehovEvaluerer {

    constructor(repositoryProvider: RepositoryProvider): this(
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    override fun definisjon(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }

    override fun ansesSomLøst(
        behandlingId: BehandlingId,
        avklaringsbehov: Avklaringsbehov,
        sakId: SakId
    ): Boolean {
        val sisteLegeerklæringBestilling = avklaringsbehov.historikk.maxBy { it.tidsstempel }

        val avvistDokumenter =
            mottattDokumentRepository.hentDokumenterAvType(
                sakId,
                InnsendingType.LEGEERKLÆRING_AVVIST
            )
        val avslåtteDokumenterEtterBestilling =
            avvistDokumenter.filter { it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel) }

        val mottatteLegeerklæringer =
            mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.LEGEERKLÆRING)
        val mottatteLegeerklæringerEtterSisteBestilling = mottatteLegeerklæringer.filter {
            it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel)
        }

        val mottattedialogmeldinger =
            mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.DIALOGMELDING)
        val mottatteDialogmeldingerEtterSisteBestilling = mottattedialogmeldinger.filter {
            it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel)
        }

        return (avslåtteDokumenterEtterBestilling.any() && avklaringsbehov.erÅpent()) ||
                mottatteLegeerklæringerEtterSisteBestilling.any() ||
                mottatteDialogmeldingerEtterSisteBestilling.any()
    }
}