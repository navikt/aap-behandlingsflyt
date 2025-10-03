package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Grunn
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class Aktivitetsplikt11_9RepositoryImplTest {
    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        @AfterAll
        @JvmStatic
        fun afterall() {
            InitTestDatabase.closerFor(dataSource)
        }
    }

    @Test
    fun `Lagrer ned og henter vurdering av aktivitetsplikt § 11-9`() {
        dataSource.transaction { connection ->
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val aktivitetspliktRepository = Aktivitetsplikt11_9RepositoryImpl(connection)
            val vurdering = Aktivitetsplikt11_9Vurdering(
                begrunnelse = "Begrunnelse",
                brudd = Brudd.IKKE_MØTT_TIL_MØTE,
                grunn = Grunn.IKKE_RIMELIG_GRUNN,
                vurdertAv = "ident",
                dato = 1 januar 2023,
                opprettet = Instant.parse("2023-01-01T12:00:00Z"),
                vurdertIBehandling = behandling.id,
            )

            aktivitetspliktRepository.lagre(behandling.id, setOf(vurdering))
            val grunnlag = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)

            assertThat(grunnlag).isEqualTo(Aktivitetsplikt11_9Grunnlag(setOf(vurdering)))

            val vurdering2 = Aktivitetsplikt11_9Vurdering(
                begrunnelse = "Begrunnelse",
                brudd = Brudd.IKKE_MØTT_TIL_MØTE,
                grunn = Grunn.RIMELIG_GRUNN,
                vurdertAv = "ident",
                dato = 1 januar 2023,
                opprettet = Instant.parse("2023-01-01T13:00:00Z"),
                vurdertIBehandling = behandling.id,
            )
         
            aktivitetspliktRepository.lagre(
                behandling.id,
                setOf(vurdering, vurdering2)
            )

            val grunnlag2 = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)
            assertThat(grunnlag2).isEqualTo(Aktivitetsplikt11_9Grunnlag(setOf(vurdering, vurdering2)))
        }
    }
}
