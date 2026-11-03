package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ÅrsakBeregningstidspunkt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskrav as ArenaRefusjonskravKontrakt

class ArenaMigreringMapperTest {

    private val fraArena = ArenaSykdomsvurdering(
        vedtakId = 1,
        begrunnelse = "Bruker oppfyller vilkåret for 11-5",
        vilkår = listOf(
            ArenaVilkår(id = 1, kode = "INNTNEDS", status = "J", begrunnelse = null),
            ArenaVilkår(id = 2, kode = "SYKSKADLYT", status = "J", begrunnelse = null),
            ArenaVilkår(id = 3, kode = "AAARBEVNE", status = "J", begrunnelse = null),
        ),
        diagnoser = listOf(
            ArenaDiagnose(
                kodeverk = "ICD-10",
                kode = "M797",
                type = "HOVED",
                opprettet = LocalDate.of(2016, 1, 1),
            ),
            ArenaDiagnose(
                kodeverk = "ICD-10",
                kode = "M80",
                type = "BI",
                opprettet = LocalDate.of(2016, 1, 1),
            )
        )
    )

    private val behandlingId = BehandlingId(1)
    private val fom: LocalDate = 1 januar 2024

    @Test
    fun `oppholdskrav godtar oppfylt og ikke vurdert fra Arena`() {
        listOf("J", "V").forEach { status ->
            assertThat(oppholdskrav(status).erOppfyltEllerIkkeVurdert()).isTrue()
        }
    }

    @Test
    fun `oppholdskrav avviser andre statuser og manglende vilkår`() {
        listOf("N", "U").forEach { status ->
            assertThat(oppholdskrav(status).erOppfyltEllerIkkeVurdert()).isFalse()
        }
        assertThat(ArenaOppholdskrav(vedtakId = 1, vilkår = null).erOppfyltEllerIkkeVurdert()).isFalse()
    }

    @Test
    fun `oppholdskrav avviser riktig status med feil vilkårkode`() {
        assertThat(oppholdskrav(status = "J", kode = "ANNET").erOppfyltEllerIkkeVurdert()).isFalse()
        assertThat(oppholdskrav(status = "V", kode = "ANNET").erOppfyltEllerIkkeVurdert()).isFalse()
    }

    @Test
    fun `tilOppfyltOppholdskravVurdering skiller mellom oppfylt og ikke vurdert`() {
        val oppfylt = ArenaMigreringMapper.tilOppfyltOppholdskravVurdering(
            fraArena = oppholdskrav("J"),
            behandlingId = behandlingId,
            fom = fom,
        )
        val ikkeVurdert = ArenaMigreringMapper.tilOppfyltOppholdskravVurdering(
            fraArena = oppholdskrav("V"),
            behandlingId = behandlingId,
            fom = fom,
        )

        assertThat(oppfylt.perioder.single().oppfylt).isTrue()
        assertThat(oppfylt.perioder.single().begrunnelse).contains("oppfylt i Arena")
        assertThat(ikkeVurdert.perioder.single().oppfylt).isTrue()
        assertThat(ikkeVurdert.perioder.single().begrunnelse).contains("ikke vurdert i Arena")
        assertThat(oppfylt.perioder.single().begrunnelse).doesNotContain("fritekst fra Arena")
        assertThat(ikkeVurdert.perioder.single().begrunnelse).doesNotContain("fritekst fra Arena")
    }

    @Test
    fun `forutgående medlemskap godtar kun oppfylt fra Arena`() {
        assertThat(forutgåendeMedlemskap("J").erOppfylt()).isTrue()
    }

    @Test
    fun `forutgående medlemskap avviser andre statuser, feil kode og manglende vilkår`() {
        listOf("V", "N", "U").forEach { status ->
            assertThat(forutgåendeMedlemskap(status).erOppfylt()).isFalse()
        }
        assertThat(forutgåendeMedlemskap("J", kode = "ANNET").erOppfylt()).isFalse()
        assertThat(ArenaForutgåendeMedlemskap(vedtakId = 1, vilkår = null).erOppfylt()).isFalse()
    }

    @Test
    fun `tilOppfyltForutgåendeMedlemskapVurdering mapper oppfylt vilkår`() {
        val vurdering = ArenaMigreringMapper.tilOppfyltForutgåendeMedlemskapVurdering(
            fraArena = forutgåendeMedlemskap("J"),
            behandlingId = behandlingId,
            fom = fom,
        )

        assertThat(vurdering.harForutgåendeMedlemskap).isTrue()
        assertThat(vurdering.varMedlemMedNedsattArbeidsevne).isNull()
        assertThat(vurdering.medlemMedUnntakAvMaksFemAar).isNull()
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
        assertThat(vurdering.fom).isEqualTo(fom)
        assertThat(vurdering.tom).isNull()
        assertThat(vurdering.begrunnelse).contains("oppfylt i Arena")
        assertThat(vurdering.begrunnelse).doesNotContain("fritekst fra Arena")
    }

    @Test
    fun `tilOppfyltForutgåendeMedlemskapVurdering feiler når vilkåret ikke er oppfylt`() {
        listOf("V", "N").forEach { status ->
            assertThatThrownBy {
                ArenaMigreringMapper.tilOppfyltForutgåendeMedlemskapVurdering(
                    fraArena = forutgåendeMedlemskap(status),
                    behandlingId = behandlingId,
                    fom = fom,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MEDLSKAP5")
        }
    }

    private fun forutgåendeMedlemskap(
        status: String,
        kode: String = "MEDLSKAP5",
    ) = ArenaForutgåendeMedlemskap(
        vedtakId = 1,
        vilkår = ArenaVilkår(
            id = 1,
            kode = kode,
            status = status,
            begrunnelse = "fritekst fra Arena",
        ),
    )

    private fun oppholdskrav(
        status: String,
        kode: String = "AOINORGE3",
    ) = ArenaOppholdskrav(
        vedtakId = 1,
        vilkår = ArenaVilkår(
            id = 1,
            kode = kode,
            status = status,
            begrunnelse = "fritekst fra Arena",
        ),
    )

    @Test
    fun `ArenaMigreringMapper mapper alle felter korrekt til Sykdomsvurdering fra Arena-respons`() {
        val vurdering = ArenaMigreringMapper.tilOppfyltOrdinærSykdomsvurdering(
            fraArena = fraArena,
            behandlingId = behandlingId,
            vurderingenGjelderFra = fom,
        )

        assertThat(vurdering.begrunnelse).isEqualTo(fraArena.begrunnelse)
        assertThat(vurdering.vurderingenGjelderFra).isEqualTo(fom)
        assertThat(vurdering.vurderingenGjelderTil).isNull()
        assertThat(vurdering.diagnose?.kodeverk).isEqualTo("ICD10")
        assertThat(vurdering.diagnose?.hoveddiagnose).isEqualTo("M797")
        assertThat(vurdering.diagnose?.bidiagnoser).containsExactly("M80")
        assertThat(vurdering.harSkadeSykdomEllerLyte).isTrue()
        assertThat(vurdering.erSkadeSykdomEllerLyteVesentligdel).isTrue()
        assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten).isTrue()
        assertThat(vurdering.harNedsattArbeidsevne).isEqualTo(ArbeidsevneNedsattValg.JA)
        assertThat(vurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense).isNull()
        assertThat(vurdering.yrkesskadeBegrunnelse).isNull()
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(vurdering.erAutomatiskVurdert()).isTrue()
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
        assertThat(vurdering.erOppfyltOrdinærMedUtlededeFelter()).isTrue()
    }

    @Test
    fun `mapSykdomsvurdering feiler med tydelig melding når Arena mangler hoveddiagnose`() {
        val utenHoveddiagnose = fraArena.copy(
            diagnoser = fraArena.diagnoser.filter { it.type == "BI" }
        )

        assertThatThrownBy {
            ArenaMigreringMapper.tilOppfyltOrdinærSykdomsvurdering(utenHoveddiagnose, behandlingId, fom)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("hoveddiagnose")
    }

    @Test
    fun `mapSykdomsvurdering feiler når bidiagnose har annet kodeverk enn hoveddiagnose`() {
        val medAvvikendeBidiagnose = fraArena.copy(
            diagnoser = fraArena.diagnoser + ArenaDiagnose(
                kodeverk = "ICPC2",
                kode = "L84",
                type = "BI",
                opprettet = LocalDate.of(2016, 1, 1),
            )
        )

        assertThatThrownBy {
            ArenaMigreringMapper.tilOppfyltOrdinærSykdomsvurdering(medAvvikendeBidiagnose, behandlingId, fom)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Bidiagnoser har ikke samme kodeverk")
    }

    @Test
    fun `mapSykdomsvurdering feiler når hoveddiagnose har ikke støttet kodeverk`() {
        val medAvvikendeBidiagnose = fraArena.copy(
            diagnoser = fraArena.diagnoser + ArenaDiagnose(
                kodeverk = "ICPC-1",
                kode = "L84",
                type = "HOVED",
                opprettet = LocalDate.of(2026, 1, 1),
            )
        )

        assertThatThrownBy {
            ArenaMigreringMapper.tilOppfyltOrdinærSykdomsvurdering(medAvvikendeBidiagnose, behandlingId, fom)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Hoveddiagnose har ikke støttet kodeverk i sykdomsvurdering fra Arena, støtter kun ICPC-2 og ICD-10")
    }

    @Test
    fun `ArenaMigreringMapper mapper alle felter korrekt til Bistandsvurdering fra Arena-respons`() {
        val vurdering = ArenaMigreringMapper.tilOppfyltBistandsvurdering(
            behandlingId = behandlingId,
            fom = fom,
        )

        assertThat(vurdering.begrunnelse).isEqualTo("Ikke vurdert i Arena, men oppfylles automatisk ved at bruker har ordinær AAP i Arena på migreringstidspunktet")
        assertThat(vurdering.fom).isEqualTo(fom)
        assertThat(vurdering.tom).isNull()
        assertThat(vurdering.erBehovForAktivBehandling).isTrue()
        assertThat(vurdering.erBehovForArbeidsrettetTiltak).isTrue()
        assertThat(vurdering.erBehovForAnnenOppfølging).isNull()
        assertThat(vurdering.overgangBegrunnelse).isNull()
        assertThat(vurdering.skalVurdereAapIOvergangTilArbeid).isNull()
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(vurdering.erAutomatiskVurdert()).isTrue()
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
    }

    @Test
    fun `ArenaMigreringMapper mapper ArenaKrav til MigrertKrav`() {
        val behandlingId = BehandlingId(1)
        val fraArena = ArenaKrav(
            arenaSaksnummer = "2016-123456",
            søknadsdato = LocalDate.of(2025, 1, 15),
            migreringsdato = LocalDate.of(2025, 12, 1),
            gjenståendeKvoteOrdinær = 150,
        )

        val krav = ArenaMigreringMapper.tilMigrertKrav(fraArena, behandlingId)

        assertThat(krav.virkningstidspunktArena).isEqualTo(LocalDate.of(2025, 1, 15))
        assertThat(krav.muligRettFra).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(krav.arenaSaksnummer).isEqualTo("2016-123456")
        assertThat(krav.rettighetstype).isEqualTo(MigrertRettighetstype.ORDINÆR)
        assertThat(krav.resterendeKvoteOrdinær).isEqualTo(150)
        assertThat(krav.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(krav.vurdertIBehandling).isEqualTo(behandlingId)
        assertThat(krav.begrunnelse).isEqualTo("Migrering av sak 2016-123456 fra Arena")
    }

    @Test
    fun `mapRefusjonskravVurdering gir automatisk vurdering uten krav når Arena ikke har refusjonskrav`() {
        val vurdering = ArenaMigreringMapper.tilRefusjonskravVurdering(null)

        assertThat(vurdering.harKrav).isFalse
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
    }

    @Test
    fun `tilRefusjonskravVurdering gir automatisk vurdering ved annen årsak enn REFKRAVSOS`() {
        val vurdering = ArenaMigreringMapper.tilRefusjonskravVurdering(
            ArenaRefusjonskrav("ANNET", LocalDate.of(2025, 1, 1), null)
        )

        assertThat(vurdering.harKrav).isFalse
    }

    @Test
    fun `ArenaRefusjonskravResponse tilDomene mapper alle felter og null`() {
        assertThat(ArenaRefusjonskravResponse(refusjonskrav = null).tilDomene()).isNull()

        val mappet = ArenaRefusjonskravResponse(
            ArenaRefusjonskravKontrakt("ANNET", 1 januar 2025, 31 januar 2025)
        ).tilDomene()

        assertThat(mappet).isEqualTo(ArenaRefusjonskrav("ANNET", 1 januar 2025, 31 januar 2025))
    }

    @Test
    fun `tilRefusjonskravVurdering feiler ved refusjonskrav av typen REFKRAVSOS`() {
        assertThatThrownBy {
            ArenaMigreringMapper.tilRefusjonskravVurdering(
                ArenaRefusjonskrav("REFKRAVSOS", LocalDate.of(2025, 1, 1), null)
            )
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("REFKRAVSOS")
    }

    @Test
    fun `ArenaMigreringMapper mapper manglende ordinær kvote til 0`() {
        val fraArena = ArenaKrav(
            arenaSaksnummer = "2016-123456",
            søknadsdato = LocalDate.of(2025, 1, 15),
            migreringsdato = LocalDate.of(2025, 12, 1),
            gjenståendeKvoteOrdinær = null,
        )

        assertThat(ArenaMigreringMapper.tilMigrertKrav(fraArena, BehandlingId(1)).resterendeKvoteOrdinær).isEqualTo(0)
    }

    @Test
    fun `tilVedtakslengdeVurdering lager automatisk vurdering med sluttdato fra Arena`() {
        val vurdering = ArenaMigreringMapper.tilVedtakslengdeVurdering(
            fraArena = vedtaksperiode(fraDato = 1 januar 2023, tilDato = 1 januar 2025),
            behandlingId = behandlingId,
        )

        assertThat(vurdering.sluttdato).isEqualTo(1 januar 2025)
        assertThat(vurdering.utvidetMed).isEqualTo(ÅrMedHverdager.FØRSTE_ÅR)
        assertThat(vurdering.årsaker).containsExactly(VedtakslengdeÅrsak.MAKS_ETT_ÅR)
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(behandlingId)
        assertThat(vurdering.vurdertAutomatisk).isTrue()
    }

    @Test
    fun `tilVedtakslengdeVurdering feiler uten tilDato`() {
        assertThatThrownBy {
            mapVedtakslengde(vedtaksperiode(fraDato = 1 januar 2023, tilDato = null))
        }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("tilDato")
    }

    @Test
    fun `tilBeregningstidspunktVurdering mapper dato, årsak ANNET og begrunnelse`() {
        val vurdering = ArenaMigreringMapper.tilBeregningstidspunktVurdering(
            ArenaBeregningstidspunkt(beregningstidspunkt = 1 januar 2023)
        )

        assertThat(vurdering.nedsattArbeidsevneEllerStudieevneDato).isEqualTo(1 januar 2023)
        assertThat(vurdering.årsak).isEqualTo(ÅrsakBeregningstidspunkt.ANNET)
        assertThat(vurdering.begrunnelse).isEqualTo("Migrert fra Arena")
        assertThat(vurdering.vurdertAv).isEqualTo(ARENA_MIGRERING_BRUKER)
        assertThat(vurdering.ytterligereNedsattArbeidsevneDato).isNull()
        assertThat(vurdering.ytterligereNedsattÅrsak).isNull()
    }

    private fun vedtaksperiode(fraDato: LocalDate?, tilDato: LocalDate?) =
        ArenaVedtaksperiode(vedtakId = 1, fraDato = fraDato, tilDato = tilDato)

    private fun mapVedtakslengde(vedtaksperiode: ArenaVedtaksperiode) =
        ArenaMigreringMapper.tilVedtakslengdeVurdering(
            fraArena = vedtaksperiode,
            behandlingId = behandlingId,
        )
}
