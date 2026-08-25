package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.DOM
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode as DomenePeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakDokumentRenderingTest {
    @Test
    fun `genererer dokument uten valgfrie grunnlag`() {
        val dokument = VedtakDokumentRenderer.render(grunnlag())

        assertThat(dokument.tittel)
            .isEqualTo(
                "Oppsummering av vilkårsvurderinger for sak ${behandlingMedVedtak.saksnummer} – 02. januar 2024"
            )
        assertThat(dokument.body).isNotEmpty()
    }

    @Test
    fun `viser alle vurderte vilkårstyper`() {
        val periode = DomenePeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        val vilkårsresultat = Vilkårsresultat(
            vilkår = Vilkårtype.entries.map { type ->
                Vilkår(
                    type = type,
                    vilkårsperioder = setOf(
                        Vilkårsperiode(
                            periode = periode,
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = "Vilkåret er oppfylt",
                        )
                    ),
                )
            }
        )
        val overskrifter = VedtakDokumentRenderer
            .render(grunnlag(vilkårsresultat))
            .body
            .filterIsInstance<DOM.Header>()
            .map { it.overskrift }

        val kontekst = RenderKontekst(behandlingId, listOf(behandlingMedVedtak))
        val forventedeVilkårsoverskrifter = Vilkårtype.entries.map { type ->
            "${PrettyEnum(type).render(kontekst)} (${type.hjemmel})"
        }
        assertThat(overskrifter).containsAll(forventedeVilkårsoverskrifter)
    }

    @Test
    fun `viser perioder og vurderingsdetaljer for vilkår`() {
        val oppfyltPeriode = DomenePeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        val avslåttPeriode = DomenePeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29))
        val vilkårsresultat = Vilkårsresultat(
            vilkår = listOf(
                Vilkår(
                    type = Vilkårtype.ALDERSVILKÅRET,
                    vilkårsperioder = setOf(
                        Vilkårsperiode(
                            periode = oppfyltPeriode,
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = true,
                            begrunnelse = "Aldersvilkåret er oppfylt",
                        ),
                        Vilkårsperiode(
                            periode = avslåttPeriode,
                            utfall = Utfall.IKKE_OPPFYLT,
                            begrunnelse = "Brukeren har fylt 67 år",
                            avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                        ),
                    ),
                )
            )
        )
        val vurderinger = VedtakDokumentRenderer
            .render(grunnlag(vilkårsresultat))
            .body
            .filterIsInstance<DOM.List>()
            .flatMap { it.liste }
            .associate { it[0] to it[1] }

        val kontekst = RenderKontekst(behandlingId, listOf(behandlingMedVedtak))
        assertThat(vurderinger[Periode(oppfyltPeriode).render(kontekst)])
            .contains("Utfall: OPPFYLT")
            .contains("Vurderingsmåte: Manuell")
            .contains("Begrunnelse: Aldersvilkåret er oppfylt")
        assertThat(vurderinger[Periode(avslåttPeriode).render(kontekst)])
            .contains("Utfall: IKKE_OPPFYLT")
            .contains("Vurderingsmåte: Maskinell")
            .contains("Avslagsårsak: BRUKER_OVER_67, § 11-4 1. ledd")
            .contains("Begrunnelse: Brukeren har fylt 67 år")
    }

    private val behandlingId = BehandlingId(42)
    private val behandlingReferanse = BehandlingReferanse()
    private val behandling = Behandling(
        id = behandlingId,
        forrigeBehandlingId = null,
        referanse = behandlingReferanse,
        sakId = SakId(1),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        vurderingsbehov = emptyList(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        opprettetTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
        versjon = 1,
    )

    private val behandlingMedVedtak = BehandlingMedVedtak(
        saksnummer = Saksnummer("1234567890"),
        id = behandlingId,
        forrigeBehandlingId = null,
        referanse = behandlingReferanse,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        opprettetTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
        vedtakId = VedtakId(1),
        vedtakstidspunkt = LocalDateTime.of(2024, 1, 2, 12, 0),
        virkningstidspunkt = null,
        vurderingsbehov = emptySet(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    )

    private fun grunnlag(
        vilkårsresultat: Vilkårsresultat = Vilkårsresultat(),
    ) = VedtakDokumentGrunnlag(
        saksnummer = behandlingMedVedtak.saksnummer,
        behandling = behandling,
        behandlinger = listOf(behandlingMedVedtak),
        vilkårsresultat = vilkårsresultat,
        tilkjentYtelse = Tidslinje.empty(),
        underveis = Tidslinje.empty(),
        mottatteDokumenter = emptyList(),
        beregningsgrunnlag = null,
        forrigeTilkjentYtelse = Tidslinje.empty(),
        forrigeUnderveis = Tidslinje.empty(),
        forrigeVilkårsresultat = Vilkårsresultat(),
        sykdomGrunnlag = null,
        bistandGrunnlag = null,
        studentGrunnlag = null,
        overgangUføreGrunnlag = null,
        etableringEgenVirksomhetGrunnlag = null,
        arbeidsevneGrunnlag = null,
        arbeidsopptrappingGrunnlag = null,
        overgangArbeidGrunnlag = null,
        vedtakslengdeGrunnlag = null,
        meldepliktGrunnlag = null,
        stønadsperiodeGrunnlag = null,
        barnetilleggGrunnlag = null,
        barnetilleggVurderinger = null,
        samordningGrunnlag = null,
        samordningUføreGrunnlag = null,
        rettighetstypeGrunnlag = null,
        institusjonsoppholdGrunnlag = null,
        sykepengerErstatningGrunnlag = null,
        refusjonkravVurderinger = null,
        avslag11_27Grunnlag = null,
        sykestipendGrunnlag = null,
        inntektsbortfallVurdering = null,
        aktivitetsplikt11_7Grunnlag = null,
        overstyringMeldepliktGrunnlag = null,
        manuellInntektGrunnlag = null,
        beregningVurderingGrunnlag = null,
        kravGrunnlag = null,
        rettighetsperiodeVurdering = null,
        lovvalgMedlemskapGrunnlag = null,
        forutgåendeMedlemskapGrunnlag = null,
        oppholdskravGrunnlag = null,
    )
}
