package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BistandRepositoryTest {

    private companion object {
        private val ident = Ident("123123123124")
        private val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
    }

    private lateinit var sak: Sak

    @BeforeEach
    fun setup() {
        InitTestDatabase.dataSource.transaction { connection ->
            sak = SakRepository(connection)
                .finnEllerOpprett(PersonRepository(connection).finnEllerOpprett(ident), periode)
        }
    }

    @AfterEach
    fun tilbakestill() {
        InitTestDatabase.dataSource.transaction { connection ->
            connection.execute("TRUNCATE TABLE SAK CASCADE")
        }
    }

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("begrunnelse", false))
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))

            val opplysninger =
                connection.queryList("SELECT BEGRUNNELSE FROM BISTAND_GRUNNLAG") {
                    setRowMapper { row -> row.getString("BEGRUNNELSE") }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly("en begrunnelse", "annen begrunnelse")
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("begrunnelse", false))
            bistandRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")
            val behandling2 = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling1.id, BistandVurdering("annen begrunnelse", false))
            bistandRepository.kopier(
                fraBehandling = behandling1.id,
                tilBehandling = behandling2.id
            )
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
            val bistandRepository = BistandRepository(connection)

            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(orginaltGrunnlag?.vurdering).isEqualTo(BistandVurdering("en begrunnelse", false))

            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))

            data class Opplysning(
                val behandlingId: Long,
                val begrunnelse: String,
                val erBehovForBistand: Boolean,
                val aktiv: Boolean
            )

            val opplysninger =
                connection.queryList("SELECT BEHANDLING_ID, BEGRUNNELSE, ER_BEHOV_FOR_BISTAND, AKTIV FROM BISTAND_GRUNNLAG") {
                    setRowMapper { row ->
                        Opplysning(
                            behandlingId = row.getLong("BEHANDLING_ID"),
                            begrunnelse = row.getString("BEGRUNNELSE"),
                            erBehovForBistand = row.getBoolean("ER_BEHOV_FOR_BISTAND"),
                            aktiv = row.getBoolean("AKTIV")
                        )
                    }
                }
            assertThat(opplysninger)
                .hasSize(2)
                .containsExactly(
                    Opplysning(behandling.id.toLong(), "en begrunnelse", erBehovForBistand = false, aktiv = false),
                    Opplysning(behandling.id.toLong(), "annen begrunnelse", erBehovForBistand = false, aktiv = true)
                )
        }
    }
}
