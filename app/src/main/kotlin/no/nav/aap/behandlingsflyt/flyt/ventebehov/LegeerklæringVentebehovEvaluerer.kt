package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class LegeerklæringVentebehovEvaluerer(private val connection: DBConnection): SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov, sakId: SakId): Boolean {
        val mottattDokumentRepository = MottattDokumentRepository(connection)
        val avvistDokumenter = mottattDokumentRepository.hentDokumenterAvType(sakId, Brevkode.LEGEERKLÆRING_AVVIST)

        val sisteLegeerklæringBestilling = avklaringsbehov.historikk.maxBy { it.tidsstempel }
        val avslåtteDokumenterEtterBestilling = avvistDokumenter.filter { it.mottattTidspunkt.isAfter(sisteLegeerklæringBestilling.tidsstempel)}

        return avslåtteDokumenterEtterBestilling.any() && avklaringsbehov.erÅpent()
    }
}