package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class Aktivitetsplikt11_7RepositoryImplTest {
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
    fun `Lagrer ned og henter vurdering av aktivitetsplikt § 11-7`() {
        dataSource.transaction { connection ->
            val sak = opprettSak(connection, periode)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val aktivitetspliktRepository = Aktivitetsplikt11_7RepositoryImpl(connection)
            val vurdering = Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse",
                erOppfylt = true,
                vurdertAv = "ident",
                opprettet = Instant.parse("2023-01-01T12:00:00Z")
            )

            aktivitetspliktRepository.lagre(behandling.id, vurdering)
            val grunnlag = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)

            assert(grunnlag?.vurdering == vurdering)

            val vurdering2 = Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Begrunnelse 2",
                erOppfylt = false,
                utfall = Utfall.STANS,
                vurdertAv = "ident2",
                opprettet = Instant.parse("2023-01-02T12:10:00Z")
            )
            aktivitetspliktRepository.lagre(behandling.id, vurdering2)
            val grunnlag2 = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)
            assert(grunnlag2?.vurdering == vurdering2)
        }
    }
    
}