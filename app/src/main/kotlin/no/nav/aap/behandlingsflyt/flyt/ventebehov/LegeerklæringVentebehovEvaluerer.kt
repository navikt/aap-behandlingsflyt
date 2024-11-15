package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class LegeerklæringVentebehovEvaluerer(private val connection: DBConnection): SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov): Boolean {
        val mottattDokumentRepository = MottattDokumentRepository(connection)
        val avslåtteDokumenter = mottattDokumentRepository.hentDokumenterAvType(behandlingId, Brevkode.LEGEERKLÆRING_AVSLÅTT)
        val relevanteAvslåtteDokumenter = avslåtteDokumenter.filter { it.behandlingId == behandlingId }

        return relevanteAvslåtteDokumenter.any() && avklaringsbehov.erÅpent()
    }
}