package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class SykdomsvurderingUtledningTest {

    @Nested
    inner class UtledErNedsettelseMinstHalvparten {

        @Test
        fun `returnerer lagret verdi hvis satt`() {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                erNedsettelseMinstHalvparten = ErNedsettelseMinstHalvpartenValg.JA // Overstyrer utledning
            )
            
            assertThat(vurdering.utledErNedsettelseMinstHalvparten())
                .isEqualTo(ErNedsettelseMinstHalvpartenValg.JA)
        }

        @ParameterizedTest
        @CsvSource(
            "true, true, JA",
            "true, false, JA_FORBIGÅENDE_PROBLEMER",
            "true, , JA",  // null = ikke besvart = JA
            "false, true, NEI",
            "false, false, NEI",
            "false, , NEI",
        )
        fun `utleder korrekt fra boolean-felter`(
            erMerEnnHalvparten: Boolean,
            erVissVarighet: Boolean?,
            forventetResultat: String
        ) {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = erMerEnnHalvparten,
                erNedsettelseIArbeidsevneAvEnVissVarighet = erVissVarighet,
            )
            
            assertThat(vurdering.utledErNedsettelseMinstHalvparten())
                .isEqualTo(ErNedsettelseMinstHalvpartenValg.valueOf(forventetResultat))
        }

        @Test
        fun `returnerer null når alle gamle felter er null`() {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = null,
                erNedsettelseIArbeidsevneAvEnVissVarighet = null,
            )
            
            assertThat(vurdering.utledErNedsettelseMinstHalvparten()).isNull()
        }
    }

    @Nested
    inner class UtledErNedsettelseMerEnnYrkesskadegrense {

        @Test
        fun `returnerer lagret verdi hvis satt`() {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                erNedsettelseMerEnnYrkesskadegrense = ErNedsettelseMerEnnYrkesskadegrenseValg.JA // Overstyrer utledning
            )
            
            assertThat(vurdering.utledErNedsettelseMerEnnYrkesskadegrense())
                .isEqualTo(ErNedsettelseMerEnnYrkesskadegrenseValg.JA)
        }

        @ParameterizedTest
        @CsvSource(
            "true, true, JA",
            "true, false, JA_FORBIGÅENDE_PROBLEMER",
            "true, , JA",  // null = ikke besvart = JA
            "false, true, NEI",
            "false, false, NEI",
            "false, , NEI",
        )
        fun `utleder korrekt fra boolean-felter`(
            erMerEnnYrkesskadegrense: Boolean,
            erVissVarighet: Boolean?,
            forventetResultat: String
        ) {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erMerEnnYrkesskadegrense,
                erNedsettelseIArbeidsevneAvEnVissVarighet = erVissVarighet,
            )
            
            assertThat(vurdering.utledErNedsettelseMerEnnYrkesskadegrense())
                .isEqualTo(ErNedsettelseMerEnnYrkesskadegrenseValg.valueOf(forventetResultat))
        }

        @Test
        fun `returnerer null når alle gamle felter er null`() {
            val vurdering = sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                erNedsettelseIArbeidsevneAvEnVissVarighet = null,
            )
            
            assertThat(vurdering.utledErNedsettelseMerEnnYrkesskadegrense()).isNull()
        }
    }
    
    @Nested
    inner class ErOppfyltOrdinærMedNyeFelter {

        @Test
        fun `returnerer true kun for JA`() {
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true, // -> JA
            ).erOppfyltOrdinærMedNyeFelter()).isTrue()

            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = false, // -> JA_FORBIGÅENDE_PROBLEMER
            ).erOppfyltOrdinærMedNyeFelter()).isFalse()

            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = false, // -> NEI
            ).erOppfyltOrdinærMedNyeFelter()).isFalse()
        }

        @Test
        fun `returnerer true for viss varighet null (etterfølgende periode)`() {
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = null, // -> JA (ikke besvart)
            ).erOppfyltOrdinærMedNyeFelter()).isTrue()
        }

        @Test
        fun `returnerer false når andre vilkår ikke er oppfylt`() {
            assertThat(sykdomsvurdering(
                harSkadeSykdomEllerLyte = false, // Ikke oppfylt
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            ).erOppfyltOrdinærMedNyeFelter()).isFalse()

            assertThat(sykdomsvurdering(
                erArbeidsevnenNedsatt = false, // Ikke oppfylt
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            ).erOppfyltOrdinærMedNyeFelter()).isFalse()

            assertThat(sykdomsvurdering(
                erSkadeSykdomEllerLyteVesentligdel = false, // Ikke oppfylt
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true,
            ).erOppfyltOrdinærMedNyeFelter()).isFalse()
        }
    }

    @Nested
    inner class SkalVurderesForSykepengeerstatningMedNyeFelter {
        
    }
    
    @Nested
    inner class ErOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter {

        @Test
        fun `returnerer true kun for JA på halvparten eller yrkesskadegrense`() {
            // JA på halvparten
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true, // -> JA
            ).erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isTrue()

            // JA på yrkesskadegrense
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = true, // -> JA
            ).erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isTrue()
        }

        @Test
        fun `returnerer false for JA_FORBIGÅENDE_PROBLEMER`() {
            // JA_FORBIGÅENDE_PROBLEMER på halvparten
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = false, // -> JA_FORBIGÅENDE_PROBLEMER
            ).erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isFalse()

            // JA_FORBIGÅENDE_PROBLEMER på yrkesskadegrense
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
                erNedsettelseIArbeidsevneAvEnVissVarighet = false, // -> JA_FORBIGÅENDE_PROBLEMER
            ).erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isFalse()
        }

        @Test
        fun `returnerer false for NEI`() {
            assertThat(sykdomsvurdering(
                erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
            ).erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isFalse()
        }
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean? = true,
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean? = null,
        erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg? = null,
        erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg? = null,
        erArbeidsevnenNedsatt: Boolean? = true,
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erNedsettelseMinstHalvparten = erNedsettelseMinstHalvparten,
        erNedsettelseMerEnnYrkesskadegrense = erNedsettelseMerEnnYrkesskadegrense,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = 1 januar 2024,
        vurderingenGjelderTil = null,
        vurdertAv = Bruker("Z00000"),
        opprettet = Instant.now(),
        vurdertIBehandling = BehandlingId(1L),
        diagnose = null
    )
}

