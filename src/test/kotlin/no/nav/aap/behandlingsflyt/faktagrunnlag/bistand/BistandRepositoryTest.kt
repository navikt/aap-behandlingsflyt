package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.InitTestDatabase
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BistandRepositoryTest {

    private companion object {
        private val ident = Ident("123123123129")
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

    @Test
    fun `Finner ikke bistand hvis ikke lagret`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).opprettBehandling(sak.id, listOf())

            val bistandRepository = BistandRepository(connection)
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            Assertions.assertThat(bistandGrunnlag).isNull()
        }
    }

    @Test
    fun `Lagrer og henter bistand`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection)

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("begrunnelse", false))
            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            Assertions.assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer ikke like bistand flere ganger`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection)

            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))

            val opplysninger =
                connection.queryList("SELECT BEGRUNNELSE FROM BISTAND_GRUNNLAG") {
                    setRowMapper { row -> row.getString("BEGRUNNELSE") }
                }
            Assertions.assertThat(opplysninger)
                .hasSize(2)
                .containsExactly("en begrunnelse", "annen begrunnelse")
        }
    }

    private fun behandling(connection: DBConnection): Behandling {
        val behandling = BehandlingRepository(connection).finnSisteBehandlingFor(sak.id)
        if (behandling == null || behandling.status().erAvsluttet()) {
            return BehandlingRepository(connection).opprettBehandling(sak.id, listOf())
        }
        return behandling
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = behandling(connection)
            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("begrunnelse", false))
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")

            val behandling2 = behandling(connection)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            Assertions.assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("begrunnelse", false))
        }
    }

    @Test
    fun `Kopierer bistand fra en behandling til en annen der fraBehandlingen har to versjoner av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling1 = behandling(connection)
            val bistandRepository = BistandRepository(connection)
            bistandRepository.lagre(behandling1.id, BistandVurdering("en begrunnelse", false))
            bistandRepository.lagre(behandling1.id, BistandVurdering("annen begrunnelse", false))
            connection.execute("UPDATE BEHANDLING SET status = 'AVSLUTTET'")

            val behandling2 = behandling(connection)

            val bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandling2.id)
            Assertions.assertThat(bistandGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))
        }
    }

    @Test
    fun `Lagrer nye bistandsopplysninger som ny rad og deaktiverer forrige versjon av opplysningene`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val behandling = behandling(connection)
            val bistandRepository = BistandRepository(connection)

            bistandRepository.lagre(behandling.id, BistandVurdering("en begrunnelse", false))
            val orginaltGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            Assertions.assertThat(orginaltGrunnlag?.vurdering).isEqualTo(BistandVurdering("en begrunnelse", false))

            bistandRepository.lagre(behandling.id, BistandVurdering("annen begrunnelse", false))
            val oppdatertGrunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
            Assertions.assertThat(oppdatertGrunnlag?.vurdering).isEqualTo(BistandVurdering("annen begrunnelse", false))

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
            Assertions.assertThat(opplysninger)
                .hasSize(4)
                .contains(
                    Opplysning(behandling.id.toLong(), "en begrunnelse", erBehovForBistand = false, aktiv = false),
                    Opplysning(behandling.id.toLong(), "annen begrunnelse", erBehovForBistand = false, aktiv = true)
                )
        }
    }
}