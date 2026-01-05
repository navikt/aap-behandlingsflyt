package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.vilkår.aktivitetsplikt.Aktivitetspliktvilkåret
import no.nav.aap.behandlingsflyt.behandling.vilkår.aktivitetsplikt.AktivitetspliktvilkåretGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset

class Aktivitetsplikt11_7RegelTest {

    @Test
    fun `Vilkårstidslinje for brudd på § 11-7 skal ignorere oppfylte perioder og begrenses til rettighetsperioden`() {
        val behandlingId = BehandlingId(1L)
        val grunnlag = Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                Aktivitetsplikt11_7Vurdering(
                    fom = 1 februar 2020,
                    begrunnelse = "Stans",
                    erOppfylt = false,
                    utfall = Utfall.STANS,
                    vurdertAv = "1234",
                    opprettet = (7 februar 2020).atStartOfDay().toInstant(ZoneOffset.UTC),
                    skalIgnorereVarselFrist = false,
                    vurdertIBehandling = behandlingId
                ),
                Aktivitetsplikt11_7Vurdering(
                    fom = 14 februar 2020,
                    begrunnelse = "Oppfylt",
                    erOppfylt = true,
                    vurdertAv = "1234",
                    opprettet = (15 februar 2020).atStartOfDay().toInstant(ZoneOffset.UTC),
                    skalIgnorereVarselFrist = false,
                    vurdertIBehandling = behandlingId
                ),
                Aktivitetsplikt11_7Vurdering(
                    fom = 1 juni 2021,
                    begrunnelse = "Opphør",
                    erOppfylt = false,
                    utfall = Utfall.OPPHØR,
                    vurdertAv = "1234",
                    opprettet = (15 juni 2021).atStartOfDay().toInstant(ZoneOffset.UTC),
                    skalIgnorereVarselFrist = false,
                    vurdertIBehandling = behandlingId
                ),
            )
        )

        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2023)

        val vurderinger = vurder(grunnlag, rettighetsperiode.fom)

        assertTidslinje(
            vurderinger,
            Periode(1 januar 2020, 31 januar 2020) to {
                assertEquals(null, it.avslagsårsak)
            },
            Periode(1 februar 2020, 13 februar 2020) to {
                assertEquals(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_STANS, it.avslagsårsak)
            },
            Periode(14 februar 2020, 31 mai 2021) to {
                assertEquals(null, it.avslagsårsak)
            },
            Periode(1 juni 2021, Tid.MAKS) to {
                assertEquals(Avslagsårsak.BRUDD_PÅ_AKTIVITETSPLIKT_OPPHØR, it.avslagsårsak)
            },
        )
    }

    private fun vurder(grunnlag: Aktivitetsplikt11_7Grunnlag, fraDato: LocalDate): Tidslinje<Vilkårsvurdering> {
        val vilkårsvurdering = Vilkårsresultat()
        Aktivitetspliktvilkåret(vilkårsvurdering).vurder(AktivitetspliktvilkåretGrunnlag(grunnlag, fraDato))
        return vilkårsvurdering.finnVilkår(Vilkårtype.AKTIVITETSPLIKT).tidslinje()
    }
}