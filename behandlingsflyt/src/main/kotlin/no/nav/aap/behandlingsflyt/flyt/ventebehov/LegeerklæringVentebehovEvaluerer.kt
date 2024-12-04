package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection

class LegeerklæringVentebehovEvaluerer(private val connection: DBConnection): SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean {
        val mottattDokumentRepository = MottattDokumentRepository(connection)
        val sisteLegeerklæringBestilling = avklaringsbehov.historikk.maxBy { it.tidsstempel }

        val avvistDokumenter =
            mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.LEGEERKLÆRING_AVVIST)
        val avslåtteDokumenterEtterBestilling = avvistDokumenter.filter { it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel)}

        val mottatteLegeerklæringer =
            mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.LEGEERKLÆRING)
        val mottatteLegeerklæringerEtterSisteBestilling = mottatteLegeerklæringer.filter { it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel) }

        val mottattedialogmeldinger =
            mottattDokumentRepository.hentDokumenterAvType(sakId, InnsendingType.DIALOGMELDING)
        val mottatteDialogmeldingerEtterSisteBestilling = mottattedialogmeldinger.filter { it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel) }

        return (avslåtteDokumenterEtterBestilling.any() && avklaringsbehov.erÅpent()) ||
            mottatteLegeerklæringerEtterSisteBestilling.any() ||
            mottatteDialogmeldingerEtterSisteBestilling.any()
    }
}