package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArbeidsevneGrunnlagTest {

    @Test
    fun `rekkefølge for vurderinger har ingenting å si innenfor en behandling ved opprettelse av tidslinje`() {
        val vurderinger = listOf(
            arbeidsevneVurdering(Prosent.`100_PROSENT`, 1 januar 2025),
            arbeidsevneVurdering(Prosent.`50_PROSENT`, 5 januar 2025),
        )

        assertThat(ArbeidsevneGrunnlag(vurderinger).tilTidslinje()).isEqualTo(ArbeidsevneGrunnlag(vurderinger.reversed()).tilTidslinje())
    }

    @Test
    fun `ved utleding av tidslinje vil til-dato settes til dagen før fra-dato i neste vurdering`() {
        val vurdering1 = arbeidsevneVurdering(Prosent.`100_PROSENT`, 1 januar 2025)
        val vurdering2 = arbeidsevneVurdering(Prosent.`50_PROSENT`, 5 januar 2025)

        val vurderinger = listOf(vurdering1, vurdering2)
        val vurderingerResultat = listOf(vurdering1.copy(tilDato = 4 januar 2025), vurdering2)

        assertThat(ArbeidsevneGrunnlag(vurderinger).tilTidslinje()).isEqualTo(ArbeidsevneGrunnlag(vurderingerResultat).tilTidslinje())
    }

    @Test
    fun `nye vurderinger har prioritet over gamle vurderinger med utgangspunkt i hvilke behandling de ble opprettet i`() {
        val eksisterendeVurderinger = listOf(
            arbeidsevneVurdering(Prosent.`100_PROSENT`, 2 januar 2025, null, BehandlingId(1)),
            arbeidsevneVurdering(Prosent.`50_PROSENT`, 5 januar 2025, null, BehandlingId(1)),
        )

        val nyeVurderinger = listOf(
            arbeidsevneVurdering(Prosent.`30_PROSENT`, 1 januar 2025, null, BehandlingId(2)),
        )

        val alleVurderinger = eksisterendeVurderinger + nyeVurderinger

        assertThat(ArbeidsevneGrunnlag(alleVurderinger).tilTidslinje()).isEqualTo(ArbeidsevneGrunnlag(nyeVurderinger).tilTidslinje())
    }

    @Test
    fun `eksisterende vurderinger som ikke helt overlapper med nye blir fortsatt gjeldende i ikke-overlappende periode`() {
        val vurdering1 = arbeidsevneVurdering(Prosent.`100_PROSENT`, 2 januar 2025, null, BehandlingId(1))
        val vurdering2 = arbeidsevneVurdering(Prosent.`50_PROSENT`, 5 januar 2025, null, BehandlingId(1))
        val nyVurdering = arbeidsevneVurdering(Prosent.`50_PROSENT`, 3 januar 2025, null, BehandlingId(2))

        val eksisterendeVurderinger = listOf(vurdering1, vurdering2)
        val nyeVurderinger = listOf(nyVurdering)

        val alleVurderinger = eksisterendeVurderinger + nyeVurderinger
        val forventetResultat = listOf(vurdering1.copy(tilDato = 2 januar 2025), nyVurdering)

        assertThat(ArbeidsevneGrunnlag(alleVurderinger).tilTidslinje()).isEqualTo(ArbeidsevneGrunnlag(forventetResultat).tilTidslinje())
    }

    @Test
    fun `to like vurderinger med hver sin fra-dato som overlapper blir komprimert til en vurdering`() {
        val vurdering = arbeidsevneVurdering(Prosent.`100_PROSENT`, 2 januar 2025, null, BehandlingId(1))
        val vurderinger = listOf(vurdering, vurdering.copy(fraDato = 5 januar 2025))

        assertThat(ArbeidsevneGrunnlag(vurderinger).tilTidslinje()).isEqualTo(ArbeidsevneGrunnlag(listOf(vurdering)).tilTidslinje())
    }

    private fun arbeidsevneVurdering(arbeidsevne: Prosent, fraDato: LocalDate, tilDato: LocalDate? = null, vurdertIBehandling: BehandlingId = BehandlingId(1)) = ArbeidsevneVurdering(
        UUID.randomUUID().toString(), arbeidsevne, fraDato, tilDato, vurdertIBehandling, LocalDateTime.now(), "vurdertAv"
    )
}