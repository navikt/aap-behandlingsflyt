package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SykdomGrunnlagTest {
    private val vurderingBehandling1 = sykdomsvurdering(
        opprettet = (1 januar 2020).atStartOfDay(),
        vurderingenGjelderFra = 1 januar 2020,
        vurderingenGjelderTil = null,
        vurdertIBehandling = BehandlingId(1)
    )
    private val vurderingerBehandling2 = listOf(
        sykdomsvurdering(
            opprettet = (15 mars 2020).atStartOfDay().plusHours(2),
            vurderingenGjelderFra = 10 januar 2020,
            vurderingenGjelderTil = null,
            vurdertIBehandling = BehandlingId(2),
            harSkadeSykdomEllerLyte = false
        ),
        sykdomsvurdering(
            opprettet = (15 mars 2020).atStartOfDay().plusHours(1),
            vurderingenGjelderFra = 1 mars 2020,
            vurdertIBehandling = BehandlingId(2)
        )
    )
    private val vurderingBehandling3 = sykdomsvurdering(
        opprettet = (20 mars 2020).atStartOfDay(),
        vurderingenGjelderFra = 1 februar 2020, // Overskriver andre vurdering i behandling 2 
        vurdertIBehandling = BehandlingId(3)
    )
    private val testCase = SykdomGrunnlag(
        yrkesskadevurdering = null,
        listOf(
            vurderingBehandling1,
            *vurderingerBehandling2.toTypedArray(),
            vurderingBehandling3,
        )
    )

    @Test
    fun `Skal utlede gjeldende tidslinje fra grunnlag`() {
        testCase
            .somSykdomsvurderingstidslinje(1 april 2020)
            .assertTidslinje(
                Segment(Periode(1 januar 2020, 9 januar 2020)) {
                    assertEquals(true, it.harSkadeSykdomEllerLyte)
                },
                Segment(Periode(10 januar 2020, 31 januar 2020)) {
                    assertThat(it)
                        .describedAs("Vurderinger gjort i samme behandling skal ikke overskrive hverandre")
                        .isEqualTo(vurderingerBehandling2[0])
                },
                Segment(Periode(1 februar 2020, 1 april 2020))
                {
                    assertThat(it)
                        .describedAs("Vurdering fra ny behandling skal overskrive tidligere vurderinger")
                        .isEqualTo(vurderingBehandling3)
                }
            )
    }

    @Test
    fun `Historiske sykdomsvurderinger skal inneholde alle vedtatte, selv om de er overskrevet av vurdering i denne behandlingen`() {
        assertThat(testCase.historiskeSykdomsvurderinger(BehandlingId(4)))
            .hasSize(4)
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    vurderingBehandling1,
                    *vurderingerBehandling2.toTypedArray(),
                    vurderingBehandling3
                )
            )
    }

    @Test
    fun `Gjeldende vedtatte sykdomsvurderinger skal filtere ut overskrevede perioder`() {
        assertThat(testCase.gjeldendeVedtatteSykdomsvurderinger(BehandlingId(4)))
            .hasSize(3)
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    vurderingBehandling1,
                    vurderingerBehandling2[0],
                    vurderingBehandling3
                )
            )
    }

    @Test
    fun `Skal hente ut vurderinger gjort i denne behandlingen`() {
        assertThat(testCase.sykdomsvurderingerVurdertIBehandling(BehandlingId(2)))
            .hasSize(2)
            .containsExactlyInAnyOrderElementsOf(
                vurderingerBehandling2
            )
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean = true,
        erArbeidsevnenNedsatt: Boolean = true,
        vurderingenGjelderFra: LocalDate = 1 januar 2020,
        vurderingenGjelderTil: LocalDate? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        vurdertIBehandling: BehandlingId
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = vurderingenGjelderTil,
        vurdertAv = Bruker("Z00000"),
        opprettet = opprettet.toInstant(ZoneOffset.UTC),
        vurdertIBehandling = vurdertIBehandling
    )
}
