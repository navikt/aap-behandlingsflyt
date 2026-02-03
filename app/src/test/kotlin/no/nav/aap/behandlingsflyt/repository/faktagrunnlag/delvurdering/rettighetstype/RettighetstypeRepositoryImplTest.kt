package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KravForOrdinærAap
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteVurdering
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.RettighetstypeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeMedKvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

class RettighetstypeRepositoryImplTest {

    companion object {
        private lateinit var dataSource: TestDataSource
        
        @BeforeAll
        @JvmStatic
        fun setUp() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `lagre, hente og slette rettighetstypegrunnlag`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val sak = dataSource.transaction { sak(it, rettighetsperiode) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val vurdering = KvoteOk(
            brukerKvote = Kvote.ORDINÆR,
            rettighetstypeVurdering = RettighetstypeVurdering(
                kravspesifikasjonForRettighetsType = KravForOrdinærAap,
                vilkårsvurderinger = emptyMap()
            )
        )

        val tidslinje = tidslinjeOf<KvoteVurdering>(rettighetsperiode to vurdering)


        val input = object : Faktagrunnlag {
            override fun hent() = "test"
        }

        // Lagre
        dataSource.transaction {
            RettighetstypeRepositoryImpl(it).lagre(behandling.id, tidslinje, input)
        }

        // Hent
        val uthentet = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hent(behandling.id)
        }
        assertThat(uthentet.perioder).hasSize(1)
        val expected = RettighetstypeMedKvote(
            periode = rettighetsperiode,
            rettighetstype = RettighetsType.BISTANDSBEHOV,
            avslagsårsaker = setOf(),
            brukerAvKvoter = setOf(Kvote.ORDINÆR)
        )
        assertThat(uthentet.perioder.first())
            .usingRecursiveComparison()
            .isEqualTo(expected)

        // Slett
        dataSource.transaction {
            RettighetstypeRepositoryImpl(it).slett(behandling.id)
        }

        val slettet = dataSource.transaction {
            RettighetstypeRepositoryImpl(it).hentHvisEksisterer(behandling.id)
        }
        assertThat(slettet).isNull()
    }

    @Test
    fun `Kopier`() {
        val rettighetsperiode = Periode(1 januar 2020, Tid.MAKS)
        val sak = dataSource.transaction { sak(it, rettighetsperiode) }
        val behandling = dataSource.transaction { finnEllerOpprettBehandling(it, sak) }

        val vurdering = KvoteOk(
            brukerKvote = Kvote.ORDINÆR,
            rettighetstypeVurdering = RettighetstypeVurdering(
                kravspesifikasjonForRettighetsType = KravForOrdinærAap,
                vilkårsvurderinger = emptyMap()
            )
        )

        val tidslinje = tidslinjeOf<KvoteVurdering>(rettighetsperiode to vurdering)
        val input = object : Faktagrunnlag {
            override fun hent() = "test"
        }
        // Lagre
        dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)


            RettighetstypeRepositoryImpl(connection).lagre(
                behandling.id, tidslinje, input
            )

            // Kopier
            val nyBehandling = finnEllerOpprettBehandling(connection, sak)
            RettighetstypeRepositoryImpl(connection).kopier(behandling.id, nyBehandling.id)

            // Hent kopiert
            val kopiert = RettighetstypeRepositoryImpl(connection).hent(
                nyBehandling.id
            )
            assertThat(kopiert.perioder).hasSize(1)
            val expected = RettighetstypeMedKvote(
                periode = rettighetsperiode,
                rettighetstype = RettighetsType.BISTANDSBEHOV,
                avslagsårsaker = setOf(),
                brukerAvKvoter = setOf(Kvote.ORDINÆR)
            )

            assertThat(kopiert.perioder.first())
                .usingRecursiveComparison()
                .isEqualTo(expected)

        }
    }
}