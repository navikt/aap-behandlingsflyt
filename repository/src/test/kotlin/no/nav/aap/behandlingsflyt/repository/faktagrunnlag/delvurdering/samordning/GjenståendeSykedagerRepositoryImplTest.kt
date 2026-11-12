package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedagerGrunnlag
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("db")
class GjenståendeSykedagerRepositoryImplTest {
    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    private val vedtak = GjenståendeSykedager(
        grunnlag = GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING,
        gjenståendeDager = 42,
        vurdertTilOgMed = 1 januar 2026,
    )

    @Test
    fun `lagrer og henter aktivt oppslag, og deaktiverer ved endring`() {
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak(it)) }

        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            .isNull()

        dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).lagre(behandling.id, vedtak) }
        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            .isEqualTo(vedtak)

        // Likt oppslag lagres ikke på nytt
        dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).lagre(behandling.id, vedtak) }

        val ingen = GjenståendeSykedager(GjenståendeSykedagerGrunnlag.INGEN_SYKEPENGER_SISTE_26_UKER)
        dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).lagre(behandling.id, ingen) }
        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            .isEqualTo(ingen)

        val antallRader = dataSource.transaction {
            it.queryFirst("SELECT count(*) AS antall FROM gjenstaaende_sykedager_grunnlag WHERE behandling_id = ?") {
                setParams { setLong(1, behandling.id.toLong()) }
                setRowMapper { row -> row.getInt("antall") }
            }
        }
        assertThat(antallRader).isEqualTo(2)
    }

    @Test
    fun `kopierer aktivt oppslag og tåler gjentatt sletting`() {
        val (fra, til) = dataSource.transaction {
            finnEllerOpprettBehandling(it, sak(it)) to finnEllerOpprettBehandling(it, sak(it))
        }
        dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).lagre(fra.id, vedtak) }

        dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).kopier(fra.id, til.id) }
        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(til.id) })
            .isEqualTo(vedtak)

        dataSource.transaction {
            GjenståendeSykedagerRepositoryImpl(it).slett(til.id)
            GjenståendeSykedagerRepositoryImpl(it).slett(til.id)
        }
        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(til.id) })
            .isNull()
        assertThat(dataSource.transaction { GjenståendeSykedagerRepositoryImpl(it).hentHvisEksisterer(fra.id) })
            .isEqualTo(vedtak)
    }
}
