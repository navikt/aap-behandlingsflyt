package no.nav.aap.behandlingsflyt.behandling.klage

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageGrunnlag
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageVurdering
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.auth.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class TrekkKlageServiceTest {

    @Test
    fun `klageErTrukket skal returnere false om man ikke har noen lagrede grunnlag`() {
        val repository = TrekkKlageRepositoryFake.medRespons(null)
        val service = TrekkKlageService(repository)

        assertThat(service.klageErTrukket(BehandlingId(123L))).isFalse()
    }

    @Test
    fun `klageErTrukket skal returnere false om vurderingen er å ikke trekke klagen`() {
        val repository = TrekkKlageRepositoryFake.medRespons(TrekkKlageGrunnlag(
            vurdering = TrekkKlageVurdering(
                begrunnelse = "test",
                skalTrekkes = false,
                hvorforTrekkes = null,
                vurdertAv = Bruker("123"),
                vurdert = LocalDateTime.now().toInstant(ZoneOffset.UTC)
            )
        ))
        val service = TrekkKlageService(repository)

        assertThat(service.klageErTrukket(BehandlingId(123L))).isFalse()
    }

    @Test
    fun `klageErTrukket skal returnere true om vurderingen er å trekke klagen`() {
        val repository = TrekkKlageRepositoryFake.medRespons(TrekkKlageGrunnlag(
            vurdering = TrekkKlageVurdering(
                begrunnelse = "test",
                skalTrekkes = true,
                hvorforTrekkes = TrekkKlageÅrsak.TRUKKET_AV_BRUKER,
                vurdertAv = Bruker("123"),
                vurdert = LocalDateTime.now().toInstant(ZoneOffset.UTC)
            )
        ))
        val service = TrekkKlageService(repository)

        assertThat(service.klageErTrukket(BehandlingId(123L))).isTrue()
    }

    class TrekkKlageRepositoryFake(private val trekkKlageGrunnlag: TrekkKlageGrunnlag?): TrekkKlageRepository {
        override fun lagreTrekkKlageVurdering(
            behandlingId: BehandlingId,
            vurdering: TrekkKlageVurdering
        ) {
            TODO("Not yet implemented")
        }

        override fun hentTrekkKlageGrunnlag(behandlingId: BehandlingId): TrekkKlageGrunnlag? {
            return trekkKlageGrunnlag
        }

        override fun kopier(
            fraBehandling: BehandlingId,
            tilBehandling: BehandlingId
        ) {
            TODO("Not yet implemented")
        }

        override fun slett(behandlingId: BehandlingId) {
            TODO("Not yet implemented")
        }

        companion object {
            fun medRespons(grunnlag: TrekkKlageGrunnlag?): TrekkKlageRepository =
                TrekkKlageRepositoryFake(grunnlag)
        }

    }
}