package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.opprettSak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
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
                gjelderFra = LocalDate.parse("2023-01-01"),
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
                gjelderFra = LocalDate.parse("2023-02-01"),
                opprettet = Instant.parse("2023-01-02T12:10:00Z")
            )
            aktivitetspliktRepository.lagre(behandling.id, vurdering2)
            val grunnlag2 = aktivitetspliktRepository.hentHvisEksisterer(behandling.id)
            assert(grunnlag2?.vurdering == vurdering2)
        }
    }

    @Test
    fun `Skal hente historiske vurderinger`() {
        val vurdering1 = Aktivitetsplikt11_7Vurdering(
            begrunnelse = "Begrunnelse 1",
            erOppfylt = true,
            vurdertAv = "ident1",
            gjelderFra = LocalDate.parse("2023-01-01"),
            opprettet = Instant.parse("2023-01-01T12:00:00Z")
        )
        val vurdering2 = Aktivitetsplikt11_7Vurdering(
            begrunnelse = "Begrunnelse 2",
            erOppfylt = false,
            utfall = Utfall.STANS,
            vurdertAv = "ident2",
            gjelderFra = LocalDate.parse("2023-02-01"),
            opprettet = Instant.parse("2023-01-02T12:10:00Z")
        )
        val vurdering3 = Aktivitetsplikt11_7Vurdering(
            begrunnelse = "Begrunnelse 3",
            erOppfylt = false,
            utfall = Utfall.OPPHØR,
            vurdertAv = "ident2",
            gjelderFra = LocalDate.parse("2023-03-01"),
            opprettet = Instant.parse("2023-01-02T12:20:00Z")
        )

        val sak = dataSource.transaction { connection -> opprettSak(connection, periode) }

        val (behandling1, behandling2, behandling3) = listOf(
            vurdering1,
            vurdering2,
            vurdering3
        ).map { opprettOgAvsluttBehandlingMedVurdering(sak.id, it) }


        dataSource.transaction { connection ->
            var historiskeVurderinger = Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHistoriskeVurderinger(sak.id, behandling1.id)
            assertThat(historiskeVurderinger).isEmpty()

            historiskeVurderinger = Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHistoriskeVurderinger(sak.id, behandling2.id)
            assertThat(historiskeVurderinger).size().isEqualTo(1)
            assertThat(historiskeVurderinger).containsExactly(vurdering1)

            historiskeVurderinger = Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHistoriskeVurderinger(behandling3.sakId, behandling3.id)
            assertThat(historiskeVurderinger).size().isEqualTo(2)
            assertThat(historiskeVurderinger).containsAll(listOf(vurdering1, vurdering2))
        }
    }


    private fun opprettOgAvsluttBehandlingMedVurdering(
        sakId: SakId,
        vurdering: Aktivitetsplikt11_7Vurdering
    ): Behandling {
        return dataSource.transaction { connection ->
            val repo = BehandlingRepositoryImpl(connection)
            val behandling = repo.opprettBehandling(
                sakId,
                TypeBehandling.Aktivitetsplikt,
                null,
                VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                    vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            type = Vurderingsbehov.AKTIVITETSPLIKT_11_7,
                            null
                        )
                    )
                )
            )

            Aktivitetsplikt11_7RepositoryImpl(connection).lagre(behandling.id, vurdering)
            repo.oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)
            repo.hent(behandling.id)
        }
    }
}
