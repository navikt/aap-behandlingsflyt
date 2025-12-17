package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktGrunnlagTest {

    @Test
    fun `rekkefølge for vurderinger har ingenting å si innenfor en behandling ved opprettelse av tidslinje`() {
        val vurderinger = listOf(
            fritaksvurdering(true, 2 januar 2025),
            fritaksvurdering(false, 3 januar 2025),
            fritaksvurdering(false, 1 januar 2025),
        )

        assertThat(MeldepliktGrunnlag(vurderinger).tilTidslinje()).isEqualTo(MeldepliktGrunnlag(vurderinger.reversed()).tilTidslinje())
    }

    @Test
    fun `ved utleding av tidslinje vil til-dato settes til dagen før fra-dato i neste vurdering`() {
        val vurdering1 = fritaksvurdering(true, 1 januar 2025)
        val vurdering2 = fritaksvurdering(false, 3 januar 2025)

        val vurderinger = listOf(vurdering1, vurdering2)
        val vurderingerResultat = listOf(vurdering1.copy(tilDato = 2 januar 2025), vurdering2)

        assertThat(MeldepliktGrunnlag(vurderinger).tilTidslinje()).isEqualTo(MeldepliktGrunnlag(vurderingerResultat).tilTidslinje())
    }

    @Test
    fun `nye vurderinger har prioritet over gamle vurderinger med utgangspunkt i hvilke behandling de ble opprettet i`() {
        val eksisterendeVurderinger = listOf(
            fritaksvurdering(true, 2 januar 2025, null, BehandlingId(1)),
            fritaksvurdering(false, 3 januar 2025, null, BehandlingId(1)),
        )

        val nyeVurderinger = listOf(
            fritaksvurdering(true, 1 januar 2025, null, BehandlingId(2)),
        )

        val alleVurderinger = eksisterendeVurderinger + nyeVurderinger

        assertThat(MeldepliktGrunnlag(alleVurderinger).tilTidslinje()).isEqualTo(MeldepliktGrunnlag(nyeVurderinger).tilTidslinje())
    }

    @Test
    fun `eksisterende vurderinger som ikke helt overlapper med nye blir fortsatt gjeldende i ikke-overlappende periode`() {
        val vurdering1 = fritaksvurdering(true, 2 januar 2025, null, BehandlingId(1))
        val vurdering2 = fritaksvurdering(false, 4 januar 2025, null, BehandlingId(1))
        val nyVurdering = fritaksvurdering(true, 3 januar 2025, null, BehandlingId(2))

        val eksisterendeVurderinger = listOf(vurdering1, vurdering2)
        val nyeVurderinger = listOf(nyVurdering)

        val alleVurderinger = eksisterendeVurderinger + nyeVurderinger
        val forventetResultat = listOf(vurdering1.copy(tilDato = 2 januar 2025), nyVurdering)

        assertThat(MeldepliktGrunnlag(alleVurderinger).tilTidslinje()).isEqualTo(MeldepliktGrunnlag(forventetResultat).tilTidslinje())
    }

    @Test
    fun `to like vurderinger med hver sin fra-dato som overlapper blir komprimert til en vurdering`() {
        val vurdering = fritaksvurdering(true, 1 januar 2025)
        val vurderinger = listOf(vurdering, vurdering.copy(fraDato = 2 januar 2025))

        assertThat(MeldepliktGrunnlag(vurderinger).tilTidslinje()).isEqualTo(MeldepliktGrunnlag(listOf(vurdering)).tilTidslinje())
    }

    private fun fritaksvurdering(harFritak: Boolean, fraDato: LocalDate, tilDato: LocalDate? = null, vurdertIBehandling: BehandlingId = BehandlingId(1)) = Fritaksvurdering(
        harFritak = harFritak,
        fraDato = fraDato,
        tilDato = tilDato,
        begrunnelse = "begrunnelse",
        vurdertAv = "saksbehandler",
        opprettetTid = LocalDateTime.now(),
        vurdertIBehandling = vurdertIBehandling
    )
}