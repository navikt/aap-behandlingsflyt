package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DelvurderingForVurderingTest {
    private val januar = LocalDate.of(2025, 1, 1)
    private val juni = LocalDate.of(2025, 6, 1)

    private val delvurderinger = listOf(
        delvurdering(januar, kanBehandlesAutomatisk = true),
        delvurdering(juni, kanBehandlesAutomatisk = false),
    )

    @Test
    fun `velger delvurderingen med relevant dato lik vurderingens fom`() {
        assertThat(delvurderinger.delvurderingForVurdering(juni)?.kanBehandlesAutomatisk).isFalse()
        assertThat(delvurderinger.delvurderingForVurdering(januar)?.kanBehandlesAutomatisk).isTrue()
    }

    @Test
    fun `velger siste delvurdering før vurderingens fom når ingen treffer eksakt`() {
        assertThat(delvurderinger.delvurderingForVurdering(LocalDate.of(2025, 3, 15))?.kanBehandlesAutomatisk).isTrue()
        assertThat(delvurderinger.delvurderingForVurdering(LocalDate.of(2025, 9, 1))?.kanBehandlesAutomatisk).isFalse()
    }

    @Test
    fun `gir null når alle delvurderinger er etter vurderingens fom eller listen er tom`() {
        assertThat(delvurderinger.delvurderingForVurdering(LocalDate.of(2024, 12, 31))).isNull()
        assertThat(emptyList<LovvalgDelvurderingerGrunnlag>().delvurderingForVurdering(januar)).isNull()
    }

    private fun delvurdering(relevantDato: LocalDate, kanBehandlesAutomatisk: Boolean) =
        LovvalgDelvurderingerGrunnlag(
            kanBehandlesAutomatisk = kanBehandlesAutomatisk,
            tilhørighetVurdering = emptyList(),
            relevantDato = relevantDato,
            vurdertIBehandling = BehandlingId(1),
        )
}
