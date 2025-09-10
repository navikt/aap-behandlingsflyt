package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Aktivitetsplikt11_7Regel.Companion.tilAktivitetspliktVurderingTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class Aktivitetsplikt11_7RegelTest {

    @Test
    fun `Vilkårstidslinje for brudd på § 11-7 skal ignorere oppfylte perioder og begrenses til rettighetsperioden`() {
        val grunnlag = Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                Aktivitetsplikt11_7Vurdering(
                    gjelderFra = 1 februar 2020,
                    begrunnelse = "Stans",
                    erOppfylt = false,
                    utfall = Utfall.STANS,
                    vurdertAv = "1234",
                    opprettet = (7 februar 2020).atStartOfDay().toInstant(ZoneOffset.UTC)
                ),
                Aktivitetsplikt11_7Vurdering(
                    gjelderFra = 14 februar 2020,
                    begrunnelse = "Oppfylt",
                    erOppfylt = true,
                    vurdertAv = "1234",
                    opprettet = (15 februar 2020).atStartOfDay().toInstant(ZoneOffset.UTC)
                ),
                Aktivitetsplikt11_7Vurdering(
                    gjelderFra = 1 juni 2021,
                    begrunnelse = "Opphør",
                    erOppfylt = false,
                    utfall = Utfall.OPPHØR,
                    vurdertAv = "1234",
                    opprettet = (15 juni 2021).atStartOfDay().toInstant(ZoneOffset.UTC)
                ),
            )
        )
        
        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2023)

        val vurderinger = grunnlag.tilAktivitetspliktVurderingTidslinje(rettighetsperiode)

        assertEquals(2, vurderinger.segmenter().size)
        
        vurderinger.assertTidslinje(
            Segment(Periode(1 februar 2020, 13 februar 2020)) {
                assertEquals(
                    it.vilkårsvurdering,
                    AktivitetspliktVurdering.Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_STANS
                )
            },
            Segment(Periode(1 juni 2021, rettighetsperiode.tom)) {
                assertEquals(
                    it.vilkårsvurdering,
                    AktivitetspliktVurdering.Vilkårsvurdering.BRUDD_AKTIVITETSPLIKT_11_7_OPPHØR
                )
            },
        )
    }
}