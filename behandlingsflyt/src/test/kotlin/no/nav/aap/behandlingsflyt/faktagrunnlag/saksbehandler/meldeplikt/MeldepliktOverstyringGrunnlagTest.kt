package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldepliktOverstyringGrunnlagTest {
    @Test
    fun `skal lage en tidslinje som inkluderer alle perioder i vurderingene`() {
        val periode1 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juni 2020,
            tom = 14 juni 2020,
            begrunnelse = "begrunnelse1",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode2 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juli 2020,
            tom = 14 juli  2020,
            begrunnelse = "begrunnelse2",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode3 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 august  2020,
            tom = 14 august 2020,
            begrunnelse = "begrunnelse3",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )


        val vurdering1 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker1",
            opprettetTid = LocalDateTime.now(),
            perioder = listOf(periode1, periode2),
            vurdertIBehandling = BehandlingReferanse()
        )

        val vurdering2 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker2",
            opprettetTid = LocalDateTime.now(),
            perioder = listOf(periode3),
            vurdertIBehandling = BehandlingReferanse()
        )

        val grunnlag = OverstyringMeldepliktGrunnlag(vurderinger = listOf(vurdering1, vurdering2))
        val tidlinje = grunnlag.tilTidslinje()

        assertThat(tidlinje.perioder()).hasSize(3)
    }

    @Test
    fun `om 2 vurderinger har ulike vurderinger for samme periode skal den siste være gjeldende`() {
        val periode1 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juni 2020,
            tom = 14 juni 2020,
            begrunnelse = "begrunnelse1",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode2 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juli 2020,
            tom = 14 juli  2020,
            begrunnelse = "begrunnelse2",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode3 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juli 2020,
            tom = 14 juli  2020,
            begrunnelse = "begrunnelse3",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG
        )


        val vurdering1 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker1",
            opprettetTid = LocalDateTime.now().minusDays(2),
            perioder = listOf(periode1, periode2),
            vurdertIBehandling = BehandlingReferanse()
        )

        val vurdering2 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker2",
            opprettetTid = LocalDateTime.now().minusDays(1),
            perioder = listOf(periode3),
            vurdertIBehandling = BehandlingReferanse()
        )

        val grunnlag = OverstyringMeldepliktGrunnlag(vurderinger = listOf(vurdering1, vurdering2))
        val tidlinje = grunnlag.tilTidslinje()

        val segmenterList = tidlinje.segmenter().toList()

        assertThat(segmenterList).hasSize(2)

        assertThat(segmenterList[0].fom()).isEqualTo(periode1.fom)
        assertThat(segmenterList[0].tom()).isEqualTo(periode1.tom)
        assertThat(segmenterList[0].verdi.meldepliktOverstyringStatus).isEqualTo(periode1.meldepliktOverstyringStatus)
        assertThat(segmenterList[0].verdi.begrunnelse).isEqualTo(periode1.begrunnelse)

        assertThat(segmenterList[1].fom()).isEqualTo(periode3.fom)
        assertThat(segmenterList[1].tom()).isEqualTo(periode3.tom)
        assertThat(segmenterList[1].verdi.meldepliktOverstyringStatus).isEqualTo(periode3.meldepliktOverstyringStatus)
        assertThat(segmenterList[1].verdi.begrunnelse).isEqualTo(periode3.begrunnelse)
    }

    @Test
    fun `om 2 vurderinger og den nyeste vurderingen delvis overskriver en gammel vurdering skal perioden deles opp i flere segmenter`() {
        val periode1 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juni 2020,
            tom = 14 juni 2020,
            begrunnelse = "begrunnelse1",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode2 = OverstyringMeldepliktVurderingPeriode(
            fom = 1 juli 2020,
            tom = 14 juli  2020,
            begrunnelse = "begrunnelse2",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.RIMELIG_GRUNN
        )

        val periode3 = OverstyringMeldepliktVurderingPeriode(
            fom = 5 juli 2020,
            tom = 14 juli  2020,
            begrunnelse = "begrunnelse3",
            meldepliktOverstyringStatus = MeldepliktOverstyringStatus.IKKE_MELDT_SEG
        )


        val vurdering1 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker1",
            opprettetTid = LocalDateTime.now().minusDays(2),
            perioder = listOf(periode1, periode2),
            vurdertIBehandling = BehandlingReferanse()
        )

        val vurdering2 = OverstyringMeldepliktVurdering(
            vurdertAv = "bruker2",
            opprettetTid = LocalDateTime.now().minusDays(1),
            perioder = listOf(periode3),
            vurdertIBehandling = BehandlingReferanse()
        )

        val grunnlag = OverstyringMeldepliktGrunnlag(vurderinger = listOf(vurdering1, vurdering2))
        val tidlinje = grunnlag.tilTidslinje()

        val segmenterList = tidlinje.segmenter().toList()

        assertThat(segmenterList).hasSize(3)

        assertThat(segmenterList[0].fom()).isEqualTo(periode1.fom)
        assertThat(segmenterList[0].tom()).isEqualTo(periode1.tom)
        assertThat(segmenterList[0].verdi.meldepliktOverstyringStatus).isEqualTo(MeldepliktOverstyringStatus.RIMELIG_GRUNN)

        assertThat(segmenterList[1].fom()).isEqualTo(periode2.fom)
        assertThat(segmenterList[1].tom()).isEqualTo(4 juli 2020)
        assertThat(segmenterList[1].verdi.meldepliktOverstyringStatus).isEqualTo(MeldepliktOverstyringStatus.RIMELIG_GRUNN)

        assertThat(segmenterList[2].fom()).isEqualTo(periode3.fom)
        assertThat(segmenterList[2].tom()).isEqualTo(periode3.tom)
        assertThat(segmenterList[2].verdi.meldepliktOverstyringStatus).isEqualTo(MeldepliktOverstyringStatus.IKKE_MELDT_SEG)
    }
}
