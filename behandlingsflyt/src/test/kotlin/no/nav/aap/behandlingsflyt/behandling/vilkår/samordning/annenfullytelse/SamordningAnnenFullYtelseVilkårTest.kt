package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.samordning.YtelseGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SamordningAnnenFullYtelseVilkårTest {

    private val rettighetsperiode = Periode(1 januar 2026, 31 desember 2026)
    private val behandlingId = BehandlingId(1L)
    private val ref1 = Kravreferanse(UUID.randomUUID())

    private fun vurder(grunnlag: SamordningAnnenFullYtelseFaktagrunnlag): Vilkårsresultat {
        val vilkårsresultat = Vilkårsresultat(id = 1L, vilkår = emptyList())
        SamordningAnnenFullYtelseVilkår(vilkårsresultat).vurder(grunnlag)
        return vilkårsresultat
    }

    private fun grunnlag(
        samordningTidslinje: Tidslinje<SamordningGradering> = Tidslinje.Companion.empty(),
        uføreGrunnlag: SamordningUføreGrunnlag? = null,
        avslag1127: Avslag11_27Grunnlag? = null,
        kravGrunnlag: KravGrunnlag? = null,
    ) = SamordningAnnenFullYtelseFaktagrunnlag(
        rettighetsperiode = rettighetsperiode,
        samordningTidslinje = samordningTidslinje,
        samordningGrunnlag = null,
        uføreRegisterGrunnlag = null,
        uføreVurderingGrunnlag = uføreGrunnlag,
        avslag1127grunnlag = avslag1127,
        kravGrunnlag = kravGrunnlag,
    )

    private fun samordning100Prosent(periode: Periode = rettighetsperiode) =
        Tidslinje(
            periode,
            SamordningGradering(
                Prosent.Companion.`100_PROSENT`,
                listOf(YtelseGradering(Ytelse.SYKEPENGER, Prosent.Companion.`100_PROSENT`))
            )
        )

    private fun uføre100Prosent() = SamordningUføreGrunnlag(
        vurdering = SamordningUføreVurdering(
            begrunnelse = "begrunnelse uføre",
            vurderingPerioder = listOf(
                SamordningUføreVurderingPeriode(
                    rettighetsperiode.fom,
                    Prosent.Companion.`100_PROSENT`
                )
            ),
            vurdertAv = Bruker("testBruker")
        )
    )

    private fun avslag1127(skalAvslås: Boolean, periode: Periode = rettighetsperiode): Avslag11_27Grunnlag {
        val krav = RelevantKrav(
            referanse = ref1,
            journalpostId = JournalpostId("jp"),
            vurdertAv = Bruker("testBruker"),
            begrunnelse = "begrunnelse nytt krav a",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            søknadsdato = Søknadsdato(periode.fom, SøknadsdatoÅrsak.SøknadMottatt),
            overstyrMuligRettFra = null,
            muligRettFra = periode.fom,
        )
        val vurdering = Avslag11_27Vurdering(
            referanse = ref1,
            begrunnelse = "begrunnelse avslag 11-27",
            harAnnenFullYtelse = skalAvslås,
            brukersYtelse = if (skalAvslås) Ytelse.SYKEPENGER else null,
            harSykepengegrunnlagOver2G = null,
            skalAvslås1127 = skalAvslås,
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            vurdertAv = Bruker("testBruker"),
        )
        return Avslag11_27Grunnlag(listOf(vurdering))
    }

    private fun kravGrunnlag(periode: Periode = rettighetsperiode) = KravGrunnlag(
        vurderinger = setOf(
            RelevantKrav(
                referanse = ref1,
                journalpostId = JournalpostId("jp"),
                vurdertAv = Bruker("testBruker"),
                begrunnelse = "begrunnelse nytt krav b",
                vurdertIBehandling = behandlingId,
                opprettet = Instant.now(),
                søknadsdato = Søknadsdato(periode.fom, SøknadsdatoÅrsak.SøknadMottatt),
                overstyrMuligRettFra = null,
                muligRettFra = periode.fom,
            )
        )
    )

    // ── ingen grunnlag ────────────────────────────────────────────────────────

    @Test
    fun `ingen samordning og ingen avslag - gir tom tidslinje`() {
        val resultat = vurder(grunnlag())
        Assertions.assertThat(resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()).isEmpty()
    }

    // ── kun samordning ────────────────────────────────────────────────────────

    @Test
    fun `samordning 100 prosent uten avslag11_27 - gir IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(samordningTidslinje = samordning100Prosent()))
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter).hasSize(1)
        Assertions.assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        Assertions.assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    @Test
    fun `uføre 100 prosent uten avslag11_27 - gir IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(uføreGrunnlag = uføre100Prosent()))
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter).hasSize(1)
        Assertions.assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        Assertions.assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    // ── kun avslag 11-27 ─────────────────────────────────────────────────────

    @Test
    fun `avslag11_27 skalAvslås true - gir IKKE_OPPFYLT med årsak ANNEN_FULL_YTELSE_AVSLAG`() {
        val resultat = vurder(
            grunnlag(avslag1127 = avslag1127(true), kravGrunnlag = kravGrunnlag())
        )
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter).hasSize(1)
        Assertions.assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        Assertions.assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
    }

    // ── prioritering: avslag11_27 vs samordning ───────────────────────────────

    @Test
    fun `avslag11_27 IKKE_OPPFYLT prioriteres over samordning IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(
            samordningTidslinje = samordning100Prosent(),
            avslag1127 = avslag1127(true),
            kravGrunnlag = kravGrunnlag(),
        ))
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter).hasSize(1)
        Assertions.assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        // Avslag 11-27 vinner
        Assertions.assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
    }

    @Test
    fun `avslag11_27 OPPFYLT, samordning IKKE_OPPFYLT - samordning gir IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(
            samordningTidslinje = samordning100Prosent(),
            avslag1127 = avslag1127(false),
            kravGrunnlag = kravGrunnlag(),
        ))
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter).hasSize(1)
        Assertions.assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        Assertions.assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    @Test
    fun `begrenset til rettighetsperiode - perioder utenfor fjernes`() {
        val periodeUtenforRettighetsperiode = Periode(1 januar 2027, 1 februar 2027)
        val resultat = vurder(grunnlag(
            samordningTidslinje = samordning100Prosent(periodeUtenforRettighetsperiode),
        ))
        val segmenter = resultat.finnVilkår(Vilkårtype.SAMORDNING).tidslinje().segmenter()
        Assertions.assertThat(segmenter.all { it.periode.fom >= rettighetsperiode.fom }).isTrue()
        Assertions.assertThat(segmenter.all { it.periode.tom <= rettighetsperiode.tom }).isTrue()
    }
}