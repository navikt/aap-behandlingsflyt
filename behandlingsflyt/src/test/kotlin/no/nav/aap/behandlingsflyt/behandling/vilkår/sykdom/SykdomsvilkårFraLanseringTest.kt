package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.time.LocalDate

class SykdomsvilkårFraLanseringTest {

    @Test
    fun `gammel og ny logikk gir samme resultat når alle felter er oppfylt med viss varighet`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024

        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = true).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = kravdato,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(kravdato),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
        
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { it.utfall == Utfall.OPPFYLT }
    }

    @Test
    fun `gammel og ny logikk gir samme resultat for avslag med forbigående problemer`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024

        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = true).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = kravdato,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = false, // Forbigående problemer
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(kravdato),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
        
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { it.utfall == Utfall.IKKE_OPPFYLT }
    }

    @Test
    fun `gammel og ny logikk gir samme resultat for avslag med ikke nok nedsettelse`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024

        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = true).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = kravdato,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = false, // Ikke nok nedsettelse
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(kravdato),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
        
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { it.utfall == Utfall.IKKE_OPPFYLT }
    }

    @Test
    fun `gammel og ny logikk gir samme resultat når viss varighet er null (etterfølgende periode)`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024
        val vurderingGjelderFra = kravdato.plusMonths(1) // Etter kravdato, så viss varighet ignoreres

        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = true).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = vurderingGjelderFra,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null, // Ikke besvart fordi ikke første periode
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(vurderingGjelderFra),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
        
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        // Perioden før vurderingen starter gir IKKE_OPPFYLT, perioden etter gir OPPFYLT
        assertThat(vilkår.vilkårsperioder()).hasSize(2)
        assertThat(vilkår.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(vilkår.vilkårsperioder().last().utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    fun `sammenligning skjer ikke når gamle felter er null`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024

        // Dette skal ikke kaste feil, selv om det er en vurdering der alle felter er null
        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = true).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = kravdato,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = null, // Alle gamle felter er null
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(kravdato),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
    }

    @Test
    fun `sammenligning skjer ikke når feature toggle er av`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = 1 januar 2024

        // Feature toggle av = ingen sammenligning, så ingen feil
        assertDoesNotThrow {
            SykdomsvilkårFraLansering(vilkårsresultat, sammenlignMedNyLogikk = false).vurder(
                SykdomsFaktagrunnlag(
                    kravDato = kravdato,
                    sisteDagMedMuligYtelse = kravdato.plusYears(3),
                    yrkesskadevurdering = null,
                    sykdomsvurderinger = listOf(
                        sykdomsvurdering(
                            vurderingenGjelderFra = kravdato,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                        )
                    ),
                    sykepengerErstatningFaktagrunnlag = null,
                    bistandvurderingFaktagrunnlag = bistandGrunnlag(kravdato),
                    sykepengeerstatningVilkår = Tidslinje()
                )
            )
        }
    }

    @ParameterizedTest
    @CsvSource(
        "true, true, JA",
        "true, false, JA_FORBIGÅENDE_PROBLEMER",
        "true, null, JA",
        "false, true, NEI",
        "false, false, NEI",
        "false, null, NEI",
        nullValues = ["null"]
    )
    fun `utledning av nye enum-felter er konsistent med gamle boolean-felter`(
        erMerEnnHalvparten: Boolean?,
        erVissVarighet: Boolean?,
        forventetResultat: String?
    ) {
        val vurdering = sykdomsvurdering(
            vurderingenGjelderFra = 1 januar 2024,
            erNedsettelseIArbeidsevneMerEnnHalvparten = erMerEnnHalvparten,
            erNedsettelseIArbeidsevneAvEnVissVarighet = erVissVarighet,
        )

        val utledet = vurdering.utledErNedsettelseMinstHalvparten()

        if (forventetResultat == null) {
            assertThat(utledet).isNull()
        } else {
            assertThat(utledet).isEqualTo(ErNedsettelseMinstHalvpartenValg.valueOf(forventetResultat))
        }
    }

    @Test
    fun `yrkesskadegrense utledes korrekt`() {
        val vurderingMedYrkesskadeJa = sykdomsvurdering(
            vurderingenGjelderFra = 1 januar 2024,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
        )
        assertThat(vurderingMedYrkesskadeJa.utledErNedsettelseMerEnnYrkesskadegrense())
            .isEqualTo(ErNedsettelseMerEnnYrkesskadegrenseValg.JA)
        assertThat(vurderingMedYrkesskadeJa.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isTrue()

        val vurderingMedYrkesskadeForbigående = sykdomsvurdering(
            vurderingenGjelderFra = 1 januar 2024,
            erNedsettelseIArbeidsevneMerEnnHalvparten = false,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true,
            erNedsettelseIArbeidsevneAvEnVissVarighet = false,
        )
        assertThat(vurderingMedYrkesskadeForbigående.utledErNedsettelseMerEnnYrkesskadegrense())
            .isEqualTo(ErNedsettelseMerEnnYrkesskadegrenseValg.JA_FORBIGÅENDE_PROBLEMER)
        assertThat(vurderingMedYrkesskadeForbigående.erOppfyltForYrkesskadeSettBortIfraÅrsakssammenhengMedNyeFelter()).isFalse()
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean? = true,
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean? = null,
        erArbeidsevnenNedsatt: Boolean = true,
        vurderingenGjelderFra: LocalDate,
        behandlingId: BehandlingId = BehandlingId(1L)
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erNedsettelseMinstHalvparten = null,
        erNedsettelseMerEnnYrkesskadegrense = null,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = null,
        vurdertAv = Bruker("Z00000"),
        opprettet = Instant.now(),
        vurdertIBehandling = behandlingId,
        diagnose = null
    )

    private fun bistandGrunnlag(fom: LocalDate) = BistandGrunnlag(
        vurderinger = listOf(
            Bistandsvurdering(
                begrunnelse = "",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = false,
                erBehovForAnnenOppfølging = false,
                overgangBegrunnelse = null,
                skalVurdereAapIOvergangTilArbeid = null,
                vurdertAv = "Z00000",
                vurderingenGjelderFra = fom,
                tom = null,
                opprettet = Instant.now(),
                vurdertIBehandling = BehandlingId(1)
            )
        )
    )
}
