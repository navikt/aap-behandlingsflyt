package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.*
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class SykepengerErstatningGrunnlagTest {
    @Test
    fun `somTidslinje skal lage en tidslinje av vurderingene`() {
        val startDato = 1 januar 2020
        val sluttDato = 1 januar 2021

        val vurdertTidspunkt = (1 januar 2020).atStartOfDay()

        val vurderinger = listOf(
            SykepengerVurdering(
                begrunnelse = "vurdering1",
                dokumenterBruktIVurdering = emptyList(),
                harRettPå = true,
                vurdertIBehandling = BehandlingId(1L),
                grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                vurdertAv = "ident",
                vurdertTidspunkt = vurdertTidspunkt,
                gjelderFra = 1 januar 2020,
                gjelderTom = 31 januar 2020,
            ),
            SykepengerVurdering(
                begrunnelse = "vurdering2",
                dokumenterBruktIVurdering = emptyList(),
                harRettPå = false,
                vurdertIBehandling = BehandlingId(1L),
                grunn = null,
                vurdertAv = "ident",
                vurdertTidspunkt = vurdertTidspunkt,
                gjelderFra = 1 februar 2020,
                gjelderTom = null,
            )
        )

        val tidslinje = SykepengerErstatningGrunnlag(vurderinger).somTidslinje(kravDato = startDato, sisteMuligDagMedYtelse = sluttDato)

        assertTidslinje(tidslinje,
            Periode(fom = startDato, tom = 31 januar 2020) to {
                assertThat(it.harRettPå).isTrue
                assertThat(it.grunn).isEqualTo(SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR)
            },
            Periode(fom = 1 februar  2020, tom = sluttDato) to {
                assertThat(it.harRettPå).isFalse
            },
        )
    }

    @Test
    fun `somTidslinje skal la nyere vurderinger overskrive eldre vurderinger ved at vurderign3 overskriver deler av vurdering1 og vurdering2`() {
        val startDato = 1 januar 2020
        val sluttDato = 1 januar 2021

        val vurdertTidspunkt1 = (1 januar 2020).atStartOfDay()
        val vurdertTidspunkt2 = (3 januar 2020).atStartOfDay()

        val vurderinger = listOf(
            SykepengerVurdering(
                begrunnelse = "vurdering1",
                dokumenterBruktIVurdering = emptyList(),
                harRettPå = true,
                vurdertIBehandling = BehandlingId(1L),
                grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                vurdertAv = "ident",
                vurdertTidspunkt = vurdertTidspunkt1,
                gjelderFra = 1 januar 2020,
                gjelderTom = 31 mai 2020,
            ),
            SykepengerVurdering(
                begrunnelse = "vurdering2",
                dokumenterBruktIVurdering = emptyList(),
                harRettPå = false,
                vurdertIBehandling = BehandlingId(1L),
                grunn = null,
                vurdertAv = "ident",
                vurdertTidspunkt = vurdertTidspunkt1,
                gjelderFra = 1 juni 2020,
                gjelderTom = null,
            ),
            SykepengerVurdering(
                begrunnelse = "vurdering3",
                dokumenterBruktIVurdering = emptyList(),
                harRettPå = true,
                vurdertIBehandling = BehandlingId(2L),
                grunn = SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR,
                vurdertAv = "ident",
                vurdertTidspunkt = vurdertTidspunkt2,
                gjelderFra = 15 april 2020,
                gjelderTom = 15 juni 2020,
            )
        )

        val tidslinje = SykepengerErstatningGrunnlag(vurderinger).somTidslinje(kravDato = startDato, sisteMuligDagMedYtelse = sluttDato)

        assertTidslinje(tidslinje,
            Periode(fom = 1 januar 2020, tom = 14 april 2020) to {
                assertThat(it.harRettPå).isTrue
                assertThat(it.begrunnelse).isEqualTo("vurdering1")
                assertThat(it.grunn).isEqualTo(SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR)
            },
            Periode(fom = 15 april 2020, tom = 15 juni 2020) to {
                assertThat(it.harRettPå).isTrue
                assertThat(it.begrunnelse).isEqualTo("vurdering3")
                assertThat(it.grunn).isEqualTo(SykepengerGrunn.SYKEPENGER_FORTSATT_ARBEIDSUFOR)
            },
            Periode(fom = 16 juni 2020, tom = sluttDato) to {
                assertThat(it.harRettPå).isFalse
                assertThat(it.begrunnelse).isEqualTo("vurdering2")
            },
        )
    }
}