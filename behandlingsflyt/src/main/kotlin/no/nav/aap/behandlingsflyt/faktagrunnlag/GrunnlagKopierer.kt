package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
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
    private val yrkesskadeRepository = YrkesskadeRepository(connection)
    private val sykdomRepository = SykdomRepository(connection)
    private val studentRepository = StudentRepository(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)
    private val sykepengerErstatningRepository = SykepengerErstatningRepository(connection)
    private val uføreRepository = UføreRepository(connection)
    private val barnRepository = BarnRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)
    private val inntektGrunnlagRepository = InntektGrunnlagRepository(connection)
    private val samordningYtelseVurderingRepository = SamordningYtelseVurderingRepository(connection)

    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        repositoryProvider.provideAlle().forEach { repository -> repository.kopier(fraBehandlingId, tilBehandlingId) }

        yrkesskadeRepository.kopier(fraBehandlingId, tilBehandlingId)
        sykdomRepository.kopier(fraBehandlingId, tilBehandlingId)
        studentRepository.kopier(fraBehandlingId, tilBehandlingId)
        meldepliktRepository.kopier(fraBehandlingId, tilBehandlingId)
        sykepengerErstatningRepository.kopier(fraBehandlingId, tilBehandlingId)
        uføreRepository.kopier(fraBehandlingId, tilBehandlingId)
        barnRepository.kopier(fraBehandlingId, tilBehandlingId)
        institusjonsoppholdRepository.kopier(fraBehandlingId, tilBehandlingId)
        inntektGrunnlagRepository.kopier(fraBehandlingId, tilBehandlingId)
        samordningYtelseVurderingRepository.kopier(fraBehandlingId, tilBehandlingId)
    }
}
