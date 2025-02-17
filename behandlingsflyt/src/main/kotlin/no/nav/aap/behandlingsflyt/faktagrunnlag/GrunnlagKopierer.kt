package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

/**
 * Har som ansvar å sette i stand en behandling etter opprettelse
 *
 * Knytter alle opplysninger fra forrige til den nye i en immutable state
 */
class GrunnlagKopierer(connection: DBConnection) {

    private val repositoryProvider = RepositoryProvider(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
    private val inntektGrunnlagRepository = InntektGrunnlagRepository(connection)

    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        repositoryProvider.provideAlle().forEach { repository -> repository.kopier(fraBehandlingId, tilBehandlingId) }

        institusjonsoppholdRepository.kopier(fraBehandlingId, tilBehandlingId)
        inntektGrunnlagRepository.kopier(fraBehandlingId, tilBehandlingId)
    }
}
