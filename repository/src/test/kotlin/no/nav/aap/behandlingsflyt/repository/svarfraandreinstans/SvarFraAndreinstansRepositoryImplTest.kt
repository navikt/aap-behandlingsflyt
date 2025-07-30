package no.nav.aap.behandlingsflyt.repository.svarfraandreinstans

import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansKonsekvens
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.FakePdlGateway
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.svarfraanadreinstans.SvarFraAndreinstansRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SvarFraAndreinstansRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Lagrer og henter vurdering av svar fra andre instans`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            finnEllerOpprettBehandling(connection, sak)
            finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTATT_KLAGE)
            val behandling = finnEllerOpprettBehandling(connection, sak, Vurderingsbehov.MOTTATT_KABAL_HENDELSE)

            val repository = SvarFraAndreinstansRepositoryImpl(connection)
            val vurdering = SvarFraAndreinstansVurdering(
                begrunnelse = "Begrunnelse",
                konsekvens = SvarFraAndreinstansKonsekvens.INGENTING,
                vurdertAv = "saksbehandler",
                vilkårSomOmgjøres = emptyList()
            )
            // Lagre vurdering
            repository.lagre(behandling.id, vurdering)

            // Hent vurdering
            val hentetGrunnlag = repository.hentHvisEksisterer(behandling.id)

            assertThat(hentetGrunnlag).isNotNull.extracting { it!!.vurdering }.extracting(
                SvarFraAndreinstansVurdering::begrunnelse,
                SvarFraAndreinstansVurdering::konsekvens,
                SvarFraAndreinstansVurdering::vilkårSomOmgjøres,
                SvarFraAndreinstansVurdering::vurdertAv
            ).containsExactly(
                "Begrunnelse",
                SvarFraAndreinstansKonsekvens.INGENTING,
                emptyList<String>(),
                "saksbehandler"
            )
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }
}