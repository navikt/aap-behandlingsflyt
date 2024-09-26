package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class EtAnnetStedUtlederService(connection: DBConnection) {
    private val barnetilleggRepository = BarnetilleggRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    fun harBehovForAvklaringer(behandlingId: BehandlingId): BehovForAvklaringer {
        val input = konstruerInput(behandlingId)

        return utledBehov(input)
    }

    private fun utledBehov(input: EtAnnetStedInput): BehovForAvklaringer {

        TODO("Not yet implemented")
    }

    private fun konstruerInput(behandlingId: BehandlingId): EtAnnetStedInput {
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder ?: emptyList()

        val opphold = grunnlag?.opphold ?: emptyList()

        return EtAnnetStedInput(opphold, barnetillegg)
    }
}