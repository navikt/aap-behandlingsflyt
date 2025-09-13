package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.help.FakePdlGateway
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.overganguføre.OvergangUføreRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

internal class OvergangUføreRepositoryImplTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @Test
    fun `Finner ikke overgang til uføre grunnlag hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)
            val overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(behandling.id)
            assertThat(overgangUføreGrunnlag).isNull()
        }
    }

    @Test
    fun `lagrer og henter overgang uføre vurdering`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)

            val testDate = LocalDate.of(2025, 1, 1)
            val expected = OvergangUføreVurdering(
                begrunnelse = "test",
                brukerHarSøktOmUføretrygd = true,
                brukerHarFåttVedtakOmUføretrygd = "NEI",
                brukerRettPåAAP = true,
                virkningsdato = testDate,
                vurdertAv = "Saks behandler",
            )

            overgangUføreRepository.lagre(behandling.id, listOf(expected))
            val actual = overgangUføreRepository.hentHvisEksisterer(behandling.id)

            assertThat(actual?.vurderinger)
                .usingRecursiveComparison()
                .ignoringFields("opprettet")
                .isEqualTo(listOf(expected))
        }
    }

    @Test
    fun `test sletting`() {
        InitTestDatabase.freshDatabase().transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val overgangUføreRepository = OvergangUføreRepositoryImpl(connection)
            overgangUføreRepository.lagre(
                behandling.id,
                listOf(
                    OvergangUføreVurdering(
                        begrunnelse = "test",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = "NEI",
                        brukerRettPåAAP = true,
                        virkningsdato = LocalDate.now(),
                        vurdertAv = "Saks behandler",
                    )
                )
            )
            overgangUføreRepository.lagre(
                behandling.id,
                listOf(
                    OvergangUføreVurdering(
                        begrunnelse = "test",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = "NEI",
                        brukerRettPåAAP = true,
                        virkningsdato = LocalDate.now(),
                        vurdertAv = "Saks behandler",
                    )
                )
            )
            assertDoesNotThrow { overgangUføreRepository.slett(behandling.id) }
        }
    }


    private companion object {
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private fun sak(connection: DBConnection): Sak {
        return PersonOgSakService(
            FakePdlGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        ).finnEllerOpprett(ident(), periode)
    }

}