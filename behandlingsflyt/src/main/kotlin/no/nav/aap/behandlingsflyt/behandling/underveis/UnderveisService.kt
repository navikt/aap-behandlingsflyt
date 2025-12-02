package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.institusjonsopphold.InstitusjonsoppholdUtlederService
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AapEtterRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.FastsettGrenseverdiArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.InstitusjonRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MapInstitusjonoppholdTilRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.OppholdskravRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SammenstiltAktivitetspliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SoningRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.reflect.KClass

class UnderveisService(
    private val sakService: SakService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val meldekortRepository: MeldekortRepository,
    private val underveisRepository: UnderveisRepository,
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val institusjonsoppholdUtlederService: InstitusjonsoppholdUtlederService,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val overstyringMeldepliktRepository: OverstyringMeldepliktRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val oppholdskravRepository: OppholdskravGrunnlagRepository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val vedtakService: VedtakService,
    private val unleashGateway: UnleashGateway,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        sakService = SakService(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        aktivitetsplikt11_7Repository = repositoryProvider.provide(),
        institusjonsoppholdUtlederService = InstitusjonsoppholdUtlederService(repositoryProvider),
        arbeidsevneRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        meldeperiodeRepository = repositoryProvider.provide(),
        overstyringMeldepliktRepository = repositoryProvider.provide(),
        oppholdskravRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        vedtakService = VedtakService(repositoryProvider),
        unleashGateway = gatewayProvider.provide(),
    )

    private val kvoteService = KvoteService()

    companion object {
        private val regelset = listOf(
            AapEtterRegel(),
            UtledMeldeperiodeRegel(),
            InstitusjonRegel(),
            OppholdskravRegel(),
            SoningRegel(),
            MeldepliktRegel(),
            SammenstiltAktivitetspliktRegel(),
            FastsettGrenseverdiArbeidRegel(),
            GraderingArbeidRegel(),
            VarighetRegel(),
        )

        init {
            fun sjekkAvhengighet(forventetFør: KClass<*>, forventetEtter: KClass<*>) {
                val offset1 = regelset.indexOfFirst { it::class == forventetFør }
                val offset2 = regelset.indexOfFirst { it::class == forventetEtter }
                check(offset1 != -1) { "Regel ${forventetFør.qualifiedName} er ikke med" }
                check(offset2 != -1) { "Regel ${forventetEtter.qualifiedName} er ikke med" }
                check(offset1 < offset2) {
                    "Regel ${forventetFør.qualifiedName} må ha kjørt før ${forventetEtter.qualifiedName}, men er kjørt etter"
                }
            }

            sjekkAvhengighet(forventetFør = UtledMeldeperiodeRegel::class, forventetEtter = MeldepliktRegel::class)
            sjekkAvhengighet(
                forventetFør = UtledMeldeperiodeRegel::class,
                forventetEtter = SammenstiltAktivitetspliktRegel::class
            )
            sjekkAvhengighet(forventetFør = UtledMeldeperiodeRegel::class, forventetEtter = GraderingArbeidRegel::class)
            sjekkAvhengighet(forventetFør = FastsettGrenseverdiArbeidRegel::class, forventetEtter = GraderingArbeidRegel::class)
        }
    }

    fun vurder(sakId: SakId, behandlingId: BehandlingId): Tidslinje<Vurdering> {
        val input = genererInput(sakId, behandlingId)

        val vurderRegler = vurderRegler(input)
        underveisRepository.lagre(
            behandlingId,
            vurderRegler.segmenter()
                .map {
                    Underveisperiode(
                        periode = it.periode,
                        meldePeriode = it.verdi.meldeperiode(),
                        utfall = it.verdi.utfall(),
                        rettighetsType = it.verdi.rettighetsType(),
                        avslagsårsak = it.verdi.avslagsårsak(),
                        grenseverdi = it.verdi.grenseverdi(),
                        arbeidsgradering = it.verdi.arbeidsgradering(),
                        trekk = if (it.verdi.skalReduseresDagsatser()) Dagsatser(1) else Dagsatser(0),
                        brukerAvKvoter = it.verdi.varighetVurdering?.brukerAvKvoter.orEmpty(),
                        institusjonsoppholdReduksjon = if (it.verdi.institusjonVurdering?.skalReduseres == true) Prosent.`50_PROSENT` else Prosent.`0_PROSENT`,
                        meldepliktStatus = it.verdi.meldepliktVurdering?.status,
                    )
                },
            input
        )
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        return regelset.fold(tidslinjeOf(input.periodeForVurdering to Vurdering(reduksjonArbeidOverGrenseEnabled = input.reduksjonArbeidOverGrenseEnabled))) { resultat, regel ->
            regel.vurder(input, resultat).begrensetTil(input.periodeForVurdering)
        }
    }

    private fun genererInput(sakId: SakId, behandlingId: BehandlingId): UnderveisInput {
        val sak = sakService.hentSakFor(behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandlingId)
        val meldekort = meldekortGrunnlag?.meldekort().orEmpty()
        val innsendingsTidspunkt = meldekortGrunnlag?.innsendingsdatoPerMelding().orEmpty()
        val kvote = kvoteService.beregn(behandlingId)
        val utlederResultat = institusjonsoppholdUtlederService.utled(behandlingId, begrensetTilRettighetsperiode = false)

        val institusjonsopphold = MapInstitusjonoppholdTilRegel.map(utlederResultat)

        val aktivitetsplikt11_7Grunnlag = aktivitetsplikt11_7Repository.hentHvisEksisterer(behandlingId)
            ?: Aktivitetsplikt11_7Grunnlag(vurderinger = emptyList())

        val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandlingId)
            ?: ArbeidsevneGrunnlag(vurderinger = emptyList())

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandlingId)
            ?: MeldepliktGrunnlag(vurderinger = emptyList())

        val overstyringMeldepliktGrunnlag = overstyringMeldepliktRepository.hentHvisEksisterer(behandlingId)
            ?: OverstyringMeldepliktGrunnlag(vurderinger = emptyList())

        val arbeidsopptrappingPerioder = arbeidsopptrappingRepository.hentPerioder(behandlingId)

        val periodeForVurdering = utledPeriodeForUnderveisvurderinger(behandlingId, sak)
        val meldeperioder = meldeperiodeRepository.hentMeldeperioder(behandlingId, periodeForVurdering)

        val oppholdskravGrunnlag = oppholdskravRepository.hentHvisEksisterer(behandlingId)
            ?: OppholdskravGrunnlag(vurderinger = emptyList())

        val vedtaksdatoFørstegangsbehandling = vedtakService.vedtakstidspunktFørstegangsbehandling(sakId)


        return UnderveisInput(
            periodeForVurdering = periodeForVurdering,
            vilkårsresultat = vilkårsresultat,
            opptrappingPerioder = arbeidsopptrappingPerioder,
            meldekort = meldekort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvoter = kvote,
            aktivitetsplikt11_7Grunnlag = aktivitetsplikt11_7Grunnlag,
            institusjonsopphold = institusjonsopphold,
            arbeidsevneGrunnlag = arbeidsevneGrunnlag,
            meldepliktGrunnlag = meldepliktGrunnlag,
            overstyringMeldepliktGrunnlag = overstyringMeldepliktGrunnlag,
            oppholdskravGrunnlag = oppholdskravGrunnlag,
            meldeperioder = meldeperioder,
            vedtaksdatoFørstegangsbehandling = vedtaksdatoFørstegangsbehandling?.toLocalDate(),
            reduksjonArbeidOverGrenseEnabled = unleashGateway.isEnabled(BehandlingsflytFeature.ReduksjonArbeidOverGrense),
            unntakMeldepliktDesemberEnabled = unleashGateway.isEnabled(BehandlingsflytFeature.UnntakMeldepliktDesember),
        )
    }

    private fun utledPeriodeForUnderveisvurderinger(
        behandlingId: BehandlingId,
        sak: Sak
    ): Periode {
        val startdatoForBehandlingen =
            VirkningstidspunktUtleder(vilkårsresultatRepository).utledVirkningsTidspunkt(behandlingId)
                ?: sak.rettighetsperiode.fom

        /**
         * TODO: Dersom sluttdato skal utvides må det håndteres her
         */
        val sluttdatoForBehandlingen = maxOf(sak.rettighetsperiode.fom, startdatoForBehandlingen)
            .plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        /**
         * For behandlinger som har passert alle vilkår og vurderinger med kortere rettighetsperiode
         * enn "sluttdatoForBehandlingen" så vil det bli feil å vurdere underveis lenger enn faktisk rettighetsperiode.
         */
        val sluttdatoForBakoverkompabilitet = minOf(sak.rettighetsperiode.tom, sluttdatoForBehandlingen)

        return Periode(sak.rettighetsperiode.fom, sluttdatoForBakoverkompabilitet)
    }
}