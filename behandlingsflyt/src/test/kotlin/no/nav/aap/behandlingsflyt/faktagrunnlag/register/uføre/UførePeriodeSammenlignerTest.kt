package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.behandling.samordning.EndringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre.UføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UførePeriodeSammenlignerTest {

    private val dataSource = InitTestDatabase.freshDatabase()
    private val periode = Periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(2))

    val femtiProsentUføreIFjor = Uføre(LocalDate.now().minusYears(1), Prosent.`50_PROSENT`)
    val hundreProsentUføreNå = Uføre(LocalDate.now(), Prosent.`100_PROSENT`)

    @Test
    fun `når det kun finnes ett grunnlag skal periodene markeres som nye`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor))
            val vurderinger = UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandlingId)
            assertThat(vurderinger.size).isEqualTo(1)
            assertThat(vurderinger.first().endringStatus).isEqualTo(EndringStatus.NY)
        }
    }

    @Test
    fun `når et uføregrunnlag har forsvunnet skal det markeres som slettet`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor))
            uføreRepository.lagre(behandlingId, emptyList())
            val vurderinger = UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandlingId)
            assertThat(vurderinger.size).isEqualTo(1)
            assertThat(vurderinger.first().endringStatus).isEqualTo(EndringStatus.SLETTET)
        }
    }

    @Test
    fun `når et av to uføregrunnlag har forsvunnet skal det markeres som slettet`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor, hundreProsentUføreNå))
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor))
            val vurderinger = UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandlingId)
            assertThat(vurderinger.size).isEqualTo(2)
            assertThat(vurderinger.first().endringStatus).isEqualTo(EndringStatus.UENDRET)
            assertThat(vurderinger.last().endringStatus).isEqualTo(EndringStatus.SLETTET)
        }
    }

    @Test
    fun `når et nytt uføregrunnlag oppstår skal det markeres som nytt`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor))
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor, hundreProsentUføreNå))
            val vurderinger = UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandlingId)
            assertThat(vurderinger.size).isEqualTo(2)
            assertThat(vurderinger.first().endringStatus).isEqualTo(EndringStatus.UENDRET)
            assertThat(vurderinger.last().endringStatus).isEqualTo(EndringStatus.NY)
        }
    }

    @Test
    fun `når et uføregrunnlag har endret grad skal det markeres som slettet og nytt`() {
        dataSource.transaction { connection ->
            val behandlingId = finnEllerOpprettBehandling(connection, sak(connection)).id
            val uføreRepository = UføreRepositoryImpl(connection)
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor))
            uføreRepository.lagre(behandlingId, listOf(femtiProsentUføreIFjor.copy(uføregrad = Prosent.`100_PROSENT`)))
            val vurderinger = UførePeriodeSammenligner(uføreRepository).hentUføreGrunnlagMedEndretStatus(behandlingId)
            assertThat(vurderinger.size).isEqualTo(2)
            assertThat(vurderinger.first().endringStatus).isEqualTo(EndringStatus.NY)
            assertThat(vurderinger.last().endringStatus).isEqualTo(EndringStatus.SLETTET)
        }
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }
}