package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class SamordningAnnenFullYtelseVilkårTest {

    private val rettighetsperiode = Periode(1 januar 2026, 31 desember 2026)
    private val behandlingId = BehandlingId(1L)
    private val ref1 = Kravreferanse(UUID.randomUUID())

    // ── ingen grunnlag ────────────────────────────────────────────────────────

    @Test
    fun `ingen samordning og ingen avslag - gir tom tidslinje`() {
        val resultat = vurder(grunnlag())
        assertThat(resultat.segmenter()).isEmpty()
    }

    // ── kun samordning ────────────────────────────────────────────────────────

    @Test
    fun `samordning 100 prosent uten avslag11_27 - gir IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(samordningGrunnlag = samordningGrunnlag(prosent = Prosent.`100_PROSENT`)))
        val segmenter = resultat.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    @Test
    fun `skal avslå hvis samordning ytelse er 100 i en periode`() {
        val res = SamordningAnnenFullYtelseVilkår.vurder(
            SamordningAnnenFullYtelseFaktagrunnlag(
                rettighetsperiode = rettighetsperiode,
                samordningGrunnlag = samordningGrunnlag(prosent = Prosent.`100_PROSENT`),
                uføreRegisterGrunnlag = null,
                uføreVurderingGrunnlag = null,
                avslag1127grunnlag = null,
                kravGrunnlag = null
            )
        )

        assertThat(res.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `skal avslå hvis samordning uføre er 100 i en periode`() {
        val periode = Periode(1 februar 2025, 31 mars 2025)
        val rettighetsperiode = Periode(1 januar 2025, 31 desember 2025)
        val forventetPeriode = Periode(periode.fom, rettighetsperiode.tom)

        val res = SamordningAnnenFullYtelseVilkår.vurder(
            SamordningAnnenFullYtelseFaktagrunnlag(
                rettighetsperiode = rettighetsperiode,
                samordningGrunnlag = tomtSamordningYtelseVurderingGrunnlag(),
                uføreRegisterGrunnlag = null,
                uføreVurderingGrunnlag = SamordningUføreGrunnlag(
                    SamordningUføreVurdering(
                        begrunnelse = "...",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = periode.fom,
                                uføregradTilSamordning = Prosent.`100_PROSENT`
                            )
                        ),
                        vurdertAv = Bruker("...")
                    )
                ),
                avslag1127grunnlag = null,
                kravGrunnlag = null
            )
        )

        assertThat(res.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
        assertThat(res.helePerioden()).isEqualTo(forventetPeriode)
    }

    @Test
    fun `skal ikke avslå hvis samordning ytelse er 50 og samordning uføre er 50`() {
        val periode = Periode(1 februar 2025, 31 mars 2025)
        val rettighetsperiode = Periode(1 januar 2025, 31 desember 2025)
        val forventetPeriode = Periode(periode.fom, rettighetsperiode.tom)

        val res = SamordningAnnenFullYtelseVilkår.vurder(
            SamordningAnnenFullYtelseFaktagrunnlag(
                rettighetsperiode = rettighetsperiode,
                samordningGrunnlag = samordningGrunnlag(periode = periode, prosent = Prosent.`50_PROSENT`),
                uføreRegisterGrunnlag = null,
                uføreVurderingGrunnlag = SamordningUføreGrunnlag(
                    SamordningUføreVurdering(
                        begrunnelse = "...",
                        vurderingPerioder = listOf(
                            SamordningUføreVurderingPeriode(
                                virkningstidspunkt = periode.fom,
                                uføregradTilSamordning = Prosent.`50_PROSENT`
                            )
                        ),
                        vurdertAv = Bruker("...")
                    )
                ),
                avslag1127grunnlag = null,
                kravGrunnlag = null
            )
        )

        assertThat(res.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_VURDERT)
        }
        assertThat(res.helePerioden()).isEqualTo(forventetPeriode)

    }


    @Test
    fun `uføre 100 prosent uten avslag11_27 - gir IKKE_OPPFYLT`() {
        val resultat = vurder(grunnlag(uføreGrunnlag = uføre100Prosent()))
        val segmenter = resultat.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    // ── kun avslag 11-27 ─────────────────────────────────────────────────────

    @Test
    fun `avslag11_27 skalAvslås true - gir IKKE_OPPFYLT med årsak ANNEN_FULL_YTELSE_AVSLAG`() {
        val resultat = vurder(
            grunnlag(avslag1127 = avslag1127(true), kravGrunnlag = kravGrunnlag())
        )
        val segmenter = resultat.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
    }

    // ── prioritering: avslag11_27 vs samordning ───────────────────────────────

    @Test
    fun `avslag11_27 IKKE_OPPFYLT prioriteres over samordning IKKE_OPPFYLT`() {
        val resultat = vurder(
            grunnlag(
                avslag1127 = avslag1127(true),
                kravGrunnlag = kravGrunnlag(),
            )
        )
        val segmenter = resultat.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        // Avslag 11-27 vinner
        assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG)
    }

    @Test
    fun `avslag11_27 OPPFYLT, samordning IKKE_OPPFYLT - samordning gir IKKE_OPPFYLT`() {
        val resultat = vurder(
            grunnlag(
                avslag1127 = avslag1127(false),
                kravGrunnlag = kravGrunnlag(),
                samordningGrunnlag = samordningGrunnlag(prosent = Prosent.`100_PROSENT`)
            )
        )
        val segmenter = resultat.segmenter()
        assertThat(segmenter).hasSize(1)
        assertThat(segmenter.first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        assertThat(segmenter.first().verdi.avslagsårsak).isEqualTo(Avslagsårsak.ANNEN_FULL_YTELSE)
    }

    @Test
    fun `begrenset til rettighetsperiode - perioder utenfor fjernes`() {
        val periodeUtenforRettighetsperiode = Periode(1 januar 2027, 1 februar 2027)
        val resultat = vurder(
            grunnlag(
                samordningGrunnlag = samordningGrunnlag(
                    periode = periodeUtenforRettighetsperiode,
                    prosent = Prosent.`100_PROSENT`
                )
            )
        )
        val segmenter = resultat.segmenter()

        assertThat(segmenter.all { it.periode.fom >= rettighetsperiode.fom }).isTrue()
        assertThat(segmenter.all { it.periode.tom <= rettighetsperiode.tom }).isTrue()
    }

    // ── strekk avslag over helg ───────────────────────────────────────────────

    @Test
    fun `avslag fredag og mandag - helga fylles med IKKE_OPPFYLT og tidslinja er sammenhengende`() {
        val fredag = Periode(2 januar 2026, 2 januar 2026)
        val mandag = Periode(5 januar 2026, 5 januar 2026)
        val resultat = vurder(
            grunnlag(
                samordningGrunnlag = samordningToPerioder(
                    fredag to Prosent.`100_PROSENT`,
                    mandag to Prosent.`100_PROSENT`,
                )
            )
        )

        val helgeperiode = Periode(fredag.fom, mandag.tom)
        // assertTidslinje krever verdi for hele perioden - fanger dermed opp hull i helga
        assertTidslinje(
            resultat.begrensetTil(helgeperiode),
            helgeperiode to { vurdering -> assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT) },
        )
    }

    @Test
    fun `avslag kun fredag - helga fylles ikke`() {
        val fredag = Periode(2 januar 2026, 2 januar 2026)
        val mandag = Periode(5 januar 2026, 5 januar 2026)
        val resultat = vurder(
            grunnlag(
                samordningGrunnlag = samordningToPerioder(
                    fredag to Prosent.`100_PROSENT`,
                    mandag to Prosent.`50_PROSENT`,
                )
            )
        )

        assertThat(resultat.segment(3 januar 2026)).isNull()
    }

    @Test
    fun `hull som ikke er ren helg fylles ikke`() {
        val torsdag = Periode(1 januar 2026, 1 januar 2026)
        val mandag = Periode(5 januar 2026, 5 januar 2026)
        val resultat = vurder(
            grunnlag(
                samordningGrunnlag = samordningToPerioder(
                    torsdag to Prosent.`100_PROSENT`,
                    mandag to Prosent.`100_PROSENT`,
                )
            )
        )

        assertThat(resultat.segment(3 januar 2026)).isNull()
    }

    @Test
    fun `helge-segmentet er lik fredagens vurdering`() {
        val fredag = Periode(2 januar 2026, 2 januar 2026)
        val mandag = Periode(5 januar 2026, 5 januar 2026)
        val resultat = vurder(
            grunnlag(
                samordningGrunnlag = samordningToPerioder(
                    fredag to Prosent.`100_PROSENT`,
                    mandag to Prosent.`100_PROSENT`,
                )
            )
        )

        val fredagVurdering = resultat.segment(2 januar 2026)!!.verdi
        val helgVurdering = resultat.segment(3 januar 2026)!!.verdi
        assertThat(helgVurdering).usingRecursiveComparison().isEqualTo(fredagVurdering)
    }

    private fun vurder(grunnlag: SamordningAnnenFullYtelseFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        return SamordningAnnenFullYtelseVilkår.vurder(grunnlag)
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

    private fun avslag1127(skalAvslås: Boolean): Avslag11_27Grunnlag {
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
        return Avslag11_27Grunnlag(setOf(vurdering))
    }

    private fun uføre100Prosent() = SamordningUføreGrunnlag(
        vurdering = SamordningUføreVurdering(
            begrunnelse = "begrunnelse uføre",
            vurderingPerioder = listOf(
                SamordningUføreVurderingPeriode(
                    rettighetsperiode.fom,
                    Prosent.`100_PROSENT`
                )
            ),
            vurdertAv = Bruker("testBruker")
        )
    )

    private fun samordningGrunnlag(periode: Periode = rettighetsperiode, prosent: Prosent) =
        SamordningYtelseVurderingGrunnlag(
            SamordningYtelseGrunnlag(
                grunnlagId = 1L,
                ytelser = setOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.FORELDREPENGER,
                        ytelsePerioder = setOf(SamordningYtelsePeriode(periode, gradering = prosent)),
                        kilde = "..."
                    )
                )
            ), SamordningVurderingGrunnlag(
                vurderingerId = 1L,
                begrunnelse = "...",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.FORELDREPENGER,
                        vurderingPerioder = setOf(
                            SamordningVurderingPeriode(
                                periode,
                                gradering = prosent,
                                manuell = false
                            )
                        )
                    )
                ),
                vurdertAv = Bruker("..."),
                vurdertTidspunkt = LocalDateTime.now(),
            )
        )

    private fun samordningToPerioder(vararg perioder: Pair<Periode, Prosent>) =
        SamordningYtelseVurderingGrunnlag(
            SamordningYtelseGrunnlag(
                grunnlagId = 1L,
                ytelser = setOf(
                    SamordningYtelse(
                        ytelseType = Ytelse.FORELDREPENGER,
                        ytelsePerioder = perioder
                            .map { (periode, prosent) -> SamordningYtelsePeriode(periode, gradering = prosent) }
                            .toSet(),
                        kilde = "..."
                    )
                )
            ), SamordningVurderingGrunnlag(
                vurderingerId = 1L,
                begrunnelse = "...",
                vurderinger = setOf(
                    SamordningVurdering(
                        ytelseType = Ytelse.FORELDREPENGER,
                        vurderingPerioder = perioder
                            .map { (periode, prosent) ->
                                SamordningVurderingPeriode(periode, gradering = prosent, manuell = false)
                            }
                            .toSet()
                    )
                ),
                vurdertAv = Bruker("..."),
                vurdertTidspunkt = LocalDateTime.now(),
            )
        )

    private fun grunnlag(
        uføreGrunnlag: SamordningUføreGrunnlag? = null,
        avslag1127: Avslag11_27Grunnlag? = null,
        kravGrunnlag: KravGrunnlag? = null,
        samordningGrunnlag: SamordningYtelseVurderingGrunnlag? = tomtSamordningYtelseVurderingGrunnlag(),
    ) = SamordningAnnenFullYtelseFaktagrunnlag(
        rettighetsperiode = rettighetsperiode,
        samordningGrunnlag = samordningGrunnlag,
        uføreRegisterGrunnlag = null,
        uføreVurderingGrunnlag = uføreGrunnlag,
        avslag1127grunnlag = avslag1127,
        kravGrunnlag = kravGrunnlag,
    )

    fun tomtSamordningYtelseVurderingGrunnlag() = SamordningYtelseVurderingGrunnlag(
        SamordningYtelseGrunnlag(
            grunnlagId = 1L,
            ytelser = emptySet()
        ), SamordningVurderingGrunnlag(
            vurderingerId = 1L,
            begrunnelse = "...",
            vurderinger = emptySet(),
            vurdertAv = Bruker("..."),
            vurdertTidspunkt = LocalDateTime.now(),
        )
    )
}