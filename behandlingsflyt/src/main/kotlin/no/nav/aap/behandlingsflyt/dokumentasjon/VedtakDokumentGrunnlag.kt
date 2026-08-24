package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class VedtakDokumentGrunnlag(
    val saksnummer: Saksnummer,
    val behandling: Behandling,
    val behandlinger: List<BehandlingMedVedtak>,
    val vilkårsresultat: Vilkårsresultat,
    val tilkjentYtelse: Tidslinje<Tilkjent>,
    val underveis: Tidslinje<Underveisperiode>,
    val mottatteDokumenter: List<MottattDokument>,
    val beregningsgrunnlag: Beregningsgrunnlag?,
    val forrigeTilkjentYtelse: Tidslinje<Tilkjent>,
    val forrigeUnderveis: Tidslinje<Underveisperiode>,
    val forrigeVilkårsresultat: Vilkårsresultat,
    val sykdomGrunnlag: SykdomGrunnlag?,
    val bistandGrunnlag: BistandGrunnlag?,
    val studentGrunnlag: StudentGrunnlag?,
    val overgangUføreGrunnlag: OvergangUføreGrunnlag?,
    val etableringEgenVirksomhetGrunnlag: EtableringEgenVirksomhetGrunnlag?,
    val arbeidsevneGrunnlag: ArbeidsevneGrunnlag?,
    val arbeidsopptrappingGrunnlag: ArbeidsopptrappingGrunnlag?,
    val overgangArbeidGrunnlag: OvergangArbeidGrunnlag?,
    val vedtakslengdeGrunnlag: VedtakslengdeGrunnlag?,
    val meldepliktGrunnlag: MeldepliktGrunnlag?,
    val stønadsperiodeGrunnlag: StønadsperiodeGrunnlag?,
    val barnetilleggGrunnlag: BarnetilleggGrunnlag?,
    val samordningGrunnlag: SamordningGrunnlag?,
    val rettighetstypeGrunnlag: RettighetstypeGrunnlag?,
    val institusjonsoppholdGrunnlag: InstitusjonsoppholdGrunnlag?,
    val sykepengerErstatningGrunnlag: SykepengerErstatningGrunnlag?,
    val refusjonkravVurderinger: List<RefusjonkravVurdering>?,
    val overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag?,
    val manuellInntektGrunnlag: ManuellInntektGrunnlag?,
    val beregningVurderingGrunnlag: BeregningGrunnlag?,
    val lovvalgMedlemskapGrunnlag: MedlemskapArbeidInntektGrunnlag?,
    val forutgåendeMedlemskapGrunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?,
    val oppholdskravGrunnlag: OppholdskravGrunnlag?,
)
