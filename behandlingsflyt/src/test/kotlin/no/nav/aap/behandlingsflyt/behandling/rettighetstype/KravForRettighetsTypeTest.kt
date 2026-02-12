package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.BRUKER_OVER_67
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.AKTIVITETSPLIKT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.ALDERSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.BISTANDSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.GRUNNLAGET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.INNTEKTSBORTFALL
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.LOVVALG
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.MEDLEMSKAP
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.OPPHOLDSKRAV
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.OVERGANGARBEIDVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.OVERGANGUFØREVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SAMORDNING
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.STRAFFEGJENNOMFØRING
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.STUDENT
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKDOMSVILKÅRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype.SYKEPENGEERSTATNING
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KravForRettighetsTypeTest {
    @Test
    fun `gjenskap fra prod-case, samordning så ordinær aap`() {
        /* Behandlingen har en blanding av vilkårsvurderinger som går
         * til `Tid.MAKS` og som går til ett år senere. */
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.vurdertOppfylt(AKTIVITETSPLIKT, Periode(22 september 2025, Tid.MAKS))
        vilkårsresultat.vurdertOppfylt(ALDERSVILKÅRET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(BISTANDSVILKÅRET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(GRUNNLAGET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(INNTEKTSBORTFALL, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(LOVVALG, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(MEDLEMSKAP, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(OPPHOLDSKRAV, Periode(22 september 2025, Tid.MAKS))
        vilkårsresultat.ikkeVurdert(OVERGANGARBEIDVILKÅRET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.ikkeVurdert(OVERGANGUFØREVILKÅRET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertIkkeOppfylt(SAMORDNING, Periode(22 september 2025, 11 november 2025), Avslagsårsak.ANNEN_FULL_YTELSE)
        vilkårsresultat.ikkeVurdert(SAMORDNING_ANNEN_LOVGIVNING, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(STRAFFEGJENNOMFØRING, Periode(22 september 2025, Tid.MAKS))
        vilkårsresultat.ikkeVurdert(STUDENT, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertOppfylt(SYKDOMSVILKÅRET, Periode(22 september 2025, 21 september 2026))
        vilkårsresultat.vurdertIkkeOppfylt(SYKEPENGEERSTATNING, Periode(22 september 2025, 21 september 2026), Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING)

        assertThat(utledStansEllerOpphør(vilkårsresultat, rettighetsperiode = Periode(22 september 2025, 21 september 2026)))
            .isEqualTo(mapOf<LocalDate, StansEllerOpphør>())
    }

    @Test
    fun `ordinær AAP-sak hvor medlemmet blir for gammel`() {
        val kravdato = 1 januar 2025
        val sisteDagAldersvilkåretOppfylt = 5 desember 2026
        val sisteVurdering = 17 desember 2026
        val rettighetsperiode = Periode(kravdato, sisteVurdering)

        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.vurdertOppfylt(LOVVALG, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(SYKDOMSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(BISTANDSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(MEDLEMSKAP, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(GRUNNLAGET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(ALDERSVILKÅRET, Periode(kravdato, sisteDagAldersvilkåretOppfylt))
        vilkårsresultat.vurdertIkkeOppfylt(
            ALDERSVILKÅRET, Periode(sisteDagAldersvilkåretOppfylt.plusDays(1), sisteVurdering),
            BRUKER_OVER_67
        )

        assertTidslinje(
            vurderRettighetsType(vilkårsresultat),
            Periode(kravdato, sisteDagAldersvilkåretOppfylt) to {
                assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
            },
        )

        assertEquals(
            utledStansEllerOpphør(vilkårsresultat, rettighetsperiode = Periode(kravdato, Tid.MAKS)),
            mapOf(
                sisteDagAldersvilkåretOppfylt.plusDays(1) to Opphør(setOf(BRUKER_OVER_67))
            )
        )
    }

    @Test
    fun `overgang uføre hvor varighet overskrides`() {
        /* NB: den faktiske varigheten knyttet til 11-18 regnes ikke ut her, men i vilkårsvurderingen
          * av OVERGANGUFØREVILKÅRET-vurderignen. Så sluttdatoen valgt
          * her er litt tilfeldig.
         */
        val kravdato = 12 januar 2025
        val sisteVurderingsdato = 11 januar 2026
        val sisteDagOvergangUføre = 5 desember 2025

        val rettighetsperiode = Periode(kravdato, sisteVurderingsdato)

        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.vurdertOppfylt(LOVVALG, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(SYKDOMSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertIkkeOppfylt(BISTANDSVILKÅRET, rettighetsperiode, IKKE_BEHOV_FOR_OPPFOLGING)
        vilkårsresultat.vurdertOppfylt(MEDLEMSKAP, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(GRUNNLAGET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(ALDERSVILKÅRET, rettighetsperiode)
        vilkårsresultat.vurdertOppfylt(OVERGANGUFØREVILKÅRET, Periode(kravdato, sisteDagOvergangUføre))
        vilkårsresultat.vurdertIkkeOppfylt(
            OVERGANGUFØREVILKÅRET, Periode(sisteDagOvergangUføre.plusDays(1), sisteVurderingsdato),
            VARIGHET_OVERSKREDET_OVERGANG_UFORE
        )

        assertTidslinje(
            vurderRettighetsType(vilkårsresultat),
            Periode(kravdato, sisteDagOvergangUføre) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
        )

        assertEquals(
            utledStansEllerOpphør(vilkårsresultat, rettighetsperiode = Periode(kravdato, Tid.MAKS)),
            mapOf(
                sisteDagOvergangUføre.plusDays(1) to
                        /* Legg spesielt merke til at IKKE_BEHOV_FOR_OPPFOLGING
                         * (bistandsbehovet) ikke er med her, selv om det
                        * vilkåret ikke er oppfylt. Det er fordi det vilkåret ikke var
                        * årsaken til at brukeren gikk fra å ha rett til å ikke ha rett.*/
                        Opphør(setOf(VARIGHET_OVERSKREDET_OVERGANG_UFORE))
            )
        )
    }

    private fun Vilkårsresultat.vurdertOppfylt(
        vilkårtype: Vilkårtype,
        periode: Periode,
        innvilgelsesårsak: Innvilgelsesårsak? = null
    ) {
        this.leggTilHvisIkkeEksisterer(vilkårtype)
            .leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    vilkårsvurdering = Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        innvilgelsesårsak = innvilgelsesårsak,
                        manuellVurdering = false,
                        begrunnelse = ".",
                        faktagrunnlag = null,
                    ),
                )
            )
    }

    private fun Vilkårsresultat.vurdertIkkeOppfylt(
        vilkårtype: Vilkårtype,
        periode: Periode,
        avslagsårsak: Avslagsårsak
    ) {
        this.leggTilHvisIkkeEksisterer(vilkårtype)
            .leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    vilkårsvurdering = Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        avslagsårsak = avslagsårsak,
                        manuellVurdering = false,
                        begrunnelse = ".",
                        faktagrunnlag = null,
                    ),
                )
            )
    }

    private fun Vilkårsresultat.ikkeVurdert(
        vilkårtype: Vilkårtype,
        periode: Periode,
    ) {
        this.leggTilHvisIkkeEksisterer(vilkårtype)
            .leggTilVurdering(
                Vilkårsperiode(
                    periode = periode,
                    vilkårsvurdering = Vilkårsvurdering(
                        utfall = Utfall.IKKE_VURDERT,
                        innvilgelsesårsak = null,
                        manuellVurdering = false,
                        begrunnelse = ".",
                        faktagrunnlag = null,
                    ),
                )
            )
    }
}