package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.FakePdlGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime

class ArbeidsevneRepositoryImplTest {

    private companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        @JvmStatic
        @AfterAll
        fun afterAll() {
            InitTestDatabase.closerFor(dataSource)
        }
    }

    @Test
    fun `Finner ikke arbeidsevne hvis ikke lagret`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)

            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandling.id)
            assertThat(arbeidsevneGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter arbeidsevne`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val arbeidsevne = ArbeidsevneVurdering("begrunnelse", Prosent(100), LocalDate.now(), null, "vurdertAv")

            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            arbeidsevneRepository.lagre(behandling.id, listOf(arbeidsevne))
            val vurderinger = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger
            assertThat(vurderinger).hasSize(1)
            assertThat(vurderinger).containsExactly(arbeidsevne.copy(opprettetTid = vurderinger?.first()?.opprettetTid))
        }
    }

    @Test
    fun `Kopierer arbeidsevne fra en behandling til en annen`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            val arbeidsevne = ArbeidsevneVurdering("begrunnelse", Prosent(100), LocalDate.now(), null, "vurdertAv")

            arbeidsevneRepository.lagre(behandling1.id, listOf(arbeidsevne))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val vurderinger = arbeidsevneRepository.hentHvisEksisterer(behandling2.id)?.vurderinger
            assertThat(vurderinger).hasSize(1)
            assertThat(vurderinger).containsExactly(arbeidsevne.copy(opprettetTid = vurderinger?.first()?.opprettetTid))
        }
    }

    @Test
    fun `Kopiering av arbeidsevne fra en behandling uten opplysningene skal ikke føre til feil`() {
        dataSource.transaction { connection ->
            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            assertDoesNotThrow {
                arbeidsevneRepository.kopier(BehandlingId(Long.MAX_VALUE - 1), BehandlingId(Long.MAX_VALUE))
            }
        }
    }

    @Test
    fun `Kopierer arbeidsevne fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            val arbeidsevne = ArbeidsevneVurdering("begrunnelse", Prosent(100), LocalDate.now(), null, "vurdertAv")
            val arbeidsevne2 = arbeidsevne.copy(begrunnelse = "annen begrunnelse")

            arbeidsevneRepository.lagre(behandling1.id, listOf(arbeidsevne))
            arbeidsevneRepository.lagre(behandling1.id, listOf(arbeidsevne2))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            val vurderinger = arbeidsevneRepository.hentHvisEksisterer(behandling2.id)?.vurderinger
            assertThat(vurderinger).hasSize(1)
            assertThat(vurderinger).containsExactly(arbeidsevne2.copy(opprettetTid = vurderinger?.first()?.opprettetTid))
        }
    }

    @Test
    fun `Lagrer nye arbeidsevneopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        dataSource.transaction { connection ->
            val sak = sak(connection)
            val behandling = finnEllerOpprettBehandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            val arbeidsevne = ArbeidsevneVurdering("begrunnelse", Prosent(100), LocalDate.now(), null, "vurdertAv")
            val arbeidsevne2 = arbeidsevne.copy("annen begrunnelse")

            arbeidsevneRepository.lagre(behandling.id, listOf(arbeidsevne))
            val originaleVurderinger = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger

            assertThat(originaleVurderinger).hasSize(1)
            assertThat(originaleVurderinger).containsExactly(
                arbeidsevne.copy(opprettetTid = originaleVurderinger?.first()?.opprettetTid)
            )

            arbeidsevneRepository.lagre(behandling.id, listOf(arbeidsevne2))
            val oppdaterteVurderinger = arbeidsevneRepository.hentHvisEksisterer(behandling.id)?.vurderinger
            assertThat(oppdaterteVurderinger).hasSize(1)
            assertThat(oppdaterteVurderinger).containsExactly(
                arbeidsevne2.copy(opprettetTid = oppdaterteVurderinger?.first()?.opprettetTid)
            )

            data class Opplysning(
                val aktiv: Boolean,
                val begrunnelse: String,
                val andelArbeidsevne: Prosent
            )

            val opplysninger =
                connection.queryList(
                    """
                    SELECT g.AKTIV, v.BEGRUNNELSE, v.ANDEL_ARBEIDSEVNE
                    FROM BEHANDLING b
                    INNER JOIN ARBEIDSEVNE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
                    INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Opplysning(
                            aktiv = row.getBoolean("AKTIV"),
                            begrunnelse = row.getString("BEGRUNNELSE"),
                            andelArbeidsevne = Prosent(row.getInt("ANDEL_ARBEIDSEVNE"))
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(aktiv = false, begrunnelse = "begrunnelse", andelArbeidsevne = Prosent(100)),
                    Opplysning(aktiv = true, begrunnelse = "annen begrunnelse", andelArbeidsevne = Prosent(100))
                )
        }
    }

    @Test
    fun `Ved kopiering av arbeidsevneopplysninger fra en avsluttet behandling til en ny skal kun referansen kopieres, ikke hele raden`() {
        dataSource.transaction { connection ->

            val sak = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak)
            val arbeidsevneRepository = ArbeidsevneRepositoryImpl(connection)
            val arbeidsevne =
                ArbeidsevneVurdering("begrunnelse", Prosent(100), LocalDate.now(), LocalDateTime.now(), "vurdertAv")
            val arbeidsevne2 = arbeidsevne.copy("annen begrunnelse")

            arbeidsevneRepository.lagre(behandling1.id, listOf(arbeidsevne))
            arbeidsevneRepository.lagre(behandling1.id, listOf(arbeidsevne2))
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling1.id, Status.AVSLUTTET)

            val behandling2 = finnEllerOpprettBehandling(connection, sak)

            data class Opplysning(
                val behandlingId: Long,
                val aktiv: Boolean,
                val begrunnelse: String,
                val andelArbeidsevne: Prosent
            )

            data class Grunnlag(val arbeidsevneId: Long, val opplysning: Opplysning)

            val opplysninger =
                connection.queryList(
                    """
                    SELECT b.ID AS BEHANDLING_ID, a.ID AS ARBEIDSEVNE_ID, g.AKTIV, v.BEGRUNNELSE, v.ANDEL_ARBEIDSEVNE
                    FROM BEHANDLING b
                    INNER JOIN ARBEIDSEVNE_GRUNNLAG g ON b.ID = g.BEHANDLING_ID
                    INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
                    INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
                    WHERE b.SAK_ID = ?
                    """.trimIndent()
                ) {
                    setParams {
                        setLong(1, sak.id.toLong())
                    }
                    setRowMapper { row ->
                        Grunnlag(
                            arbeidsevneId = row.getLong("ARBEIDSEVNE_ID"),
                            opplysning = Opplysning(
                                behandlingId = row.getLong("BEHANDLING_ID"),
                                aktiv = row.getBoolean("AKTIV"),
                                begrunnelse = row.getString("BEGRUNNELSE"),
                                andelArbeidsevne = Prosent(row.getInt("ANDEL_ARBEIDSEVNE"))
                            )
                        )
                    }
                }
            assertThat(opplysninger.map(Grunnlag::arbeidsevneId).distinct())
                .hasSize(2)
            assertThat(opplysninger.map(Grunnlag::opplysning))
                .hasSize(3)
                .containsExactly(
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = false,
                        begrunnelse = arbeidsevne.begrunnelse,
                        andelArbeidsevne = Prosent(100)
                    ),
                    Opplysning(
                        behandlingId = behandling1.id.toLong(),
                        aktiv = true,
                        begrunnelse = arbeidsevne2.begrunnelse,
                        andelArbeidsevne = Prosent(100)
                    ),
                    Opplysning(
                        behandlingId = behandling2.id.toLong(),
                        aktiv = true,
                        begrunnelse = arbeidsevne2.begrunnelse,
                        andelArbeidsevne = Prosent(100)
                    )
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
}