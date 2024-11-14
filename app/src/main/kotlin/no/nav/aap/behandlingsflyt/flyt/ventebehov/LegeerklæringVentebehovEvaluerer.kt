package no.nav.aap.behandlingsflyt.flyt.ventebehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class LegeerklæringVentebehovEvaluerer(private val connection: DBConnection): SpesifikkVentebehovEvaluerer {
    override fun definisjon(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }

    override fun ansesSomLøst(behandlingId: BehandlingId, avklaringsbehov: Avklaringsbehov): Boolean {
        val mottattDokumentRepository = MottattDokumentRepository(connection)
        val behandlingRepository = BehandlingRepositoryImpl(connection)

        val behandling = behandlingRepository.hent(behandlingId)
        val avslåtteDokumenter = mottattDokumentRepository.hentDokumenterAvType(behandling.sakId, Brevkode.LEGEERKLÆRING_AVSLÅTT)

        val relevanteAvslåtteDokumenter = avslåtteDokumenter.filter { it.behandlingId == behandlingId }

        return relevanteAvslåtteDokumenter.any() && avklaringsbehov.erÅpent() && avklaringsbehov.grunn() == ÅrsakTilSettPåVent.VENTER_PÅ_MEDISINSKE_OPPLYSNINGER
    }
}