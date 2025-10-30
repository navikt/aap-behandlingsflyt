package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.opphold

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravPeriode
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppholdskravGrunnlagRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lagre og hent oppholdskravene i db`() {
        val behandlingId = opprettBehandling(dataSource)
        val oppholdskravVurdering = OppholdskravVurdering(
            vurdertAv = "Meg",
            vurdertIBehandling = behandlingId,
            perioder = listOf(
                OppholdskravPeriode(
                    fom = LocalDate.parse("2019-01-01"),
                    tom = null,
                    land = "Norge",
                    oppfylt = false,
                    begrunnelse = "Fordi"
                )
            )
        )

        dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).lagre(behandlingId, oppholdskravVurdering)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(grunnlag!!.vurderinger).hasSize(1)
        val vurdering = grunnlag.vurderinger.first()
        assertThat(vurdering.perioder).hasSize(1)
        assertThat(vurdering.vurdertAv).isEqualTo("Meg")

        val førstePeriode = vurdering.perioder.first()

        assertThat(førstePeriode.tom).isNull()
        assertThat(førstePeriode.fom).isEqualTo(LocalDate.parse("2019-01-01"))
        assertThat(førstePeriode.land).isEqualTo("Norge")
        assertThat(førstePeriode.begrunnelse).isEqualTo("Fordi")
        assertThat(førstePeriode.oppfylt).isFalse
    }


    @Test
    fun `slett grunnlag`() {
        val behandlingId = opprettBehandling(dataSource)
        val oppholdskravVurdering = OppholdskravVurdering(
            vurdertAv = "Meg",
            vurdertIBehandling = behandlingId,
            perioder = listOf(
                OppholdskravPeriode(
                    fom = LocalDate.parse("2019-01-01"),
                    tom = null,
                    land = "Norge",
                    oppfylt = false,
                    begrunnelse = "Fordi"
                )
            )
        )
        dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).lagre(behandlingId, oppholdskravVurdering)
        }

        val grunnlag = dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }
        assertThat(grunnlag).isNotNull
        dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).slett(behandlingId)
        }
        val ikkeGrunnlag = dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }
        assertThat(ikkeGrunnlag).isNull()
    }

    @Test
    fun `periodisering av tidslinje`() {

    }


    @Test
    fun `hent historiske oppholdskravene i db`() {


    }


    @Test
    fun `kopier grunnlag`() {
        val behandlingId = opprettBehandling(dataSource)
        val oppholdskravVurdering = OppholdskravVurdering(
            vurdertAv = "Meg",
            vurdertIBehandling = behandlingId,
            perioder = listOf(
                OppholdskravPeriode(
                    fom = LocalDate.parse("2019-01-01"),
                    tom = null,
                    land = "Norge",
                    oppfylt = false,
                    begrunnelse = "Fordi"
                )
            )
        )
        dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).lagre(behandlingId, oppholdskravVurdering)
        }
        val revurderingBehandlingId = opprettBehandling(dataSource)
        dataSource.transaction { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).kopier(behandlingId, revurderingBehandlingId)
        }

        val grunnlag = dataSource.transaction(readOnly = true) { connection ->
            OppholdskravGrunnlagRepositoryImpl(connection).hentHvisEksisterer(revurderingBehandlingId)
        }

        assertThat(grunnlag).isNotNull
        assertLikeVurdering(oppholdskravVurdering,grunnlag!!.vurderinger.first())

        assertThat(grunnlag.vurderinger).hasSize(1)

    }

    private fun assertLikeVurdering(oppholdskravVurdering1 : OppholdskravVurdering,oppholdskravVurdering2 : OppholdskravVurdering) {
        assertThat(oppholdskravVurdering1.vurdertAv).isEqualTo(oppholdskravVurdering2.vurdertAv)
        assertThat(oppholdskravVurdering1.perioder).isEqualTo(oppholdskravVurdering2.perioder)
        assertThat(oppholdskravVurdering1.vurdertIBehandling).isEqualTo(oppholdskravVurdering2.vurdertIBehandling)
    }


    private fun opprettBehandlingPåSak(dataSource: DataSource, sakId: SakId): BehandlingId {
        val behandling = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sakId,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(listOf(), ÅrsakTilOpprettelse.SØKNAD)
            )
        }
        return behandling.id
    }


    private fun opprettBehandling(dataSource: DataSource): BehandlingId {
        val person = dataSource.transaction { connection ->
            PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("01017012345")))
        }

        val behandling = dataSource.transaction { connection ->
            val sak = SakRepositoryImpl(connection).finnEllerOpprett(
                person,
                Periode(
                    fom = LocalDate.parse("2025-01-01"),
                    tom = LocalDate.parse("2025-08-01")
                )
            )

            BehandlingRepositoryImpl(connection).opprettBehandling(
                sakId = sak.id,
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                forrigeBehandlingId = null,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(listOf(), ÅrsakTilOpprettelse.SØKNAD)
            )

        }
        return behandling.id
    }
}