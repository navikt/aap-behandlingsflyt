package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class VedtakDokumentGenerator(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val barnRepository: BarnRepository,
    private val samordningRepository: SamordningRepository,
    private val rettighetstypeRepository: RettighetstypeRepository,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val refusjonkravRepository: RefusjonkravRepository,
    private val avslag11_27Repository: Avslag11_27Repository,
    private val sykestipendRepository: SykestipendRepository,
    private val inntektsbortfallRepository: InntektsbortfallRepository,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val overstyringMeldepliktRepository: OverstyringMeldepliktRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val kravRepository: KravRepository,
    private val vurderRettighetsperiodeRepository: VurderRettighetsperiodeRepository,
    private val medlemskapArbeidInntektRepository: MedlemskapArbeidInntektRepository,
    private val medlemskapArbeidInntektForutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        arbeidsevneRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        vedtakslengdeRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
        barnetilleggRepository = repositoryProvider.provide(),
        barnRepository = repositoryProvider.provide(),
        samordningRepository = repositoryProvider.provide(),
        rettighetstypeRepository = repositoryProvider.provide(),
        institusjonsoppholdRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        refusjonkravRepository = repositoryProvider.provide(),
        avslag11_27Repository = repositoryProvider.provide(),
        sykestipendRepository = repositoryProvider.provide(),
        inntektsbortfallRepository = repositoryProvider.provide(),
        aktivitetsplikt11_7Repository = repositoryProvider.provide(),
        overstyringMeldepliktRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        kravRepository = repositoryProvider.provide(),
        vurderRettighetsperiodeRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektForutgåendeRepository = repositoryProvider.provide(),
        oppholdskravGrunnlagRepository = repositoryProvider.provide(),
    )

    fun genererDokument(
        behandlingId: BehandlingId,
        sakId: SakId,
        vedtakstidspunkt: LocalDateTime,
        forrigeBehandlingId: BehandlingId?,
    ) = VedtakDokumentRenderer.render(
        lagGrunnlag(
            behandlingId = behandlingId,
            sakId = sakId,
            vedtakstidspunkt = vedtakstidspunkt,
            forrigeBehandlingId = forrigeBehandlingId,
        )
    )

    private fun lagGrunnlag(
        behandlingId: BehandlingId,
        sakId: SakId,
        vedtakstidspunkt: LocalDateTime,
        forrigeBehandlingId: BehandlingId?,
    ): VedtakDokumentGrunnlag {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val behandlinger = behandlingRepository.hentAlleMedVedtakFor(
            sak.person.id,
            TypeBehandling.ytelseBehandlingstyper()
        ).filter { it.vedtakstidspunkt <= vedtakstidspunkt.plusSeconds(1) }

        return VedtakDokumentGrunnlag(
            saksnummer = sak.saksnummer,
            behandling = behandling,
            behandlinger = behandlinger,
            vilkårsresultat = vilkårsresultatRepository.hent(behandlingId),
            tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
                ?.tilTidslinje().orEmpty(),
            underveis = underveisRepository.hentHvisEksisterer(behandlingId)?.somTidslinje().orEmpty(),
            mottatteDokumenter = mottattDokumentRepository.hentDokumenterForSak(sakId).toList(),
            beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandlingId),

            forrigeTilkjentYtelse = forrigeBehandlingId
                ?.let { tilkjentYtelseRepository.hentHvisEksisterer(it) }
                ?.tilTidslinje().orEmpty(),
            forrigeUnderveis = forrigeBehandlingId
                ?.let { underveisRepository.hentHvisEksisterer(it) }
                ?.somTidslinje().orEmpty(),
            forrigeVilkårsresultat = forrigeBehandlingId
                ?.let { vilkårsresultatRepository.hent(it) }
                ?: Vilkårsresultat(),

            sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId),
            bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId),
            studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId),
            overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(behandlingId),
            etableringEgenVirksomhetGrunnlag = etableringEgenVirksomhetRepository.hentHvisEksisterer(behandlingId),
            arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandlingId),
            arbeidsopptrappingGrunnlag = arbeidsopptrappingRepository.hentHvisEksisterer(behandlingId),
            overgangArbeidGrunnlag = overgangArbeidRepository.hentHvisEksisterer(behandlingId),
            vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandlingId),
            meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandlingId),
            stønadsperiodeGrunnlag = stønadsperiodeRepository.hentHvisEksisterer(behandlingId),
            barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandlingId),
            barnetilleggVurderinger = barnRepository.hentVurderteBarnHvisEksisterer(behandlingId),
            samordningGrunnlag = samordningRepository.hentHvisEksisterer(behandlingId),
            rettighetstypeGrunnlag = rettighetstypeRepository.hentHvisEksisterer(behandlingId),
            institusjonsoppholdGrunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId),
            sykepengerErstatningGrunnlag = sykepengerErstatningRepository.hentHvisEksisterer(behandlingId),
            refusjonkravVurderinger = refusjonkravRepository.hentHvisEksisterer(behandlingId),
            avslag11_27Grunnlag = avslag11_27Repository.hentHvisEksisterer(behandlingId),
            sykestipendGrunnlag = sykestipendRepository.hentHvisEksisterer(behandlingId),
            inntektsbortfallVurdering = inntektsbortfallRepository.hentHvisEksisterer(behandlingId),
            aktivitetsplikt11_7Grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(behandlingId),
            overstyringMeldepliktGrunnlag = overstyringMeldepliktRepository.hentHvisEksisterer(behandlingId),
            manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId),
            beregningVurderingGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId),
            kravGrunnlag = kravRepository.hentHvisEksisterer(behandlingId),
            rettighetsperiodeVurdering = vurderRettighetsperiodeRepository.hentVurdering(behandlingId),
            lovvalgMedlemskapGrunnlag = medlemskapArbeidInntektRepository.hentHvisEksisterer(behandlingId),
            forutgåendeMedlemskapGrunnlag = medlemskapArbeidInntektForutgåendeRepository.hentHvisEksisterer(
                behandlingId
            ),
            oppholdskravGrunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(behandlingId),
        )
    }
}
