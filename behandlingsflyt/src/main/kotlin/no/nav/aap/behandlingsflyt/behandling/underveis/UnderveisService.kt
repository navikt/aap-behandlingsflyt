package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.AapEtterRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.InstitusjonRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MapInstitusjonoppholdTilRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SammenstiltAktivitetspliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SoningRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.reflect.KClass

class UnderveisService(
    private val behandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val meldekortRepository: MeldekortRepository,
    private val underveisRepository: UnderveisRepository,
    private val aktivitetspliktRepository: AktivitetspliktRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val meldepliktRimeligGrunnRepository: MeldepliktRimeligGrunnRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val vedtakService: VedtakService,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): this(
        behandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        aktivitetspliktRepository = repositoryProvider.provide(),
        etAnnetStedUtlederService = EtAnnetStedUtlederService(repositoryProvider),
        arbeidsevneRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        meldeperiodeRepository = repositoryProvider.provide(),
        meldepliktRimeligGrunnRepository = repositoryProvider.provide(),
        vedtakService = VedtakService(repositoryProvider),
    )

    private val kvoteService = KvoteService()

    companion object {
        private val regelset = listOf(
            AapEtterRegel(),
            UtledMeldeperiodeRegel(),
            InstitusjonRegel(),
            SoningRegel(),
            MeldepliktRegel(),
            SammenstiltAktivitetspliktRegel(),
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
                        bruddAktivitetspliktId = it.verdi.aktivitetspliktVurdering?.dokument?.metadata?.id,
                        institusjonsoppholdReduksjon = if (it.verdi.institusjonVurdering?.skalReduseres == true) Prosent.`50_PROSENT` else Prosent.`0_PROSENT`,
                        meldepliktStatus = it.verdi.meldepliktVurdering?.status,
                    )
                },
            input
        )
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        return regelset.fold(Tidslinje()) { resultat, regel ->
            regel.vurder(input, resultat).begrensetTil(input.rettighetsperiode)
        }
    }

    private fun genererInput(sakId: SakId, behandlingId: BehandlingId): UnderveisInput {
        val sak = behandlingService.hentSakFor(behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val meldekortGrunnlag = meldekortRepository.hentHvisEksisterer(behandlingId)
        val meldekort = meldekortGrunnlag?.meldekort().orEmpty()
        val innsendingsTidspunkt = meldekortGrunnlag?.innsendingsdatoPerMelding().orEmpty()
        val kvote = kvoteService.beregn(behandlingId)
        val utlederResultat = etAnnetStedUtlederService.utled(behandlingId)

        val etAnnetSted = MapInstitusjonoppholdTilRegel.map(utlederResultat)

        val aktivitetspliktGrunnlag = aktivitetspliktRepository.hentGrunnlagHvisEksisterer(behandlingId)
            ?: AktivitetspliktGrunnlag(bruddene = setOf())

        val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandlingId)
            ?: ArbeidsevneGrunnlag(vurderinger = emptyList())

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandlingId)
            ?: MeldepliktGrunnlag(vurderinger = emptyList())

        val meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnRepository.hentHvisEksisterer(behandlingId)
            ?: MeldepliktRimeligGrunnGrunnlag(vurderinger = emptyList())

        val meldeperioder = meldeperiodeRepository.hent(behandlingId)

        val vedtaksdatoFørstegangsbehandling = vedtakService.vedtakstidspunktFørstegangsbehandling(sakId)

        return UnderveisInput(
            rettighetsperiode = sak.rettighetsperiode,
            vilkårsresultat = vilkårsresultat,
            opptrappingPerioder = emptyList(),
            meldekort = meldekort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvoter = kvote,
            aktivitetspliktGrunnlag = aktivitetspliktGrunnlag,
            etAnnetSted = etAnnetSted,
            arbeidsevneGrunnlag = arbeidsevneGrunnlag,
            meldepliktGrunnlag = meldepliktGrunnlag,
            meldepliktRimeligGrunnGrunnlag = meldepliktRimeligGrunnGrunnlag,
            meldeperioder = meldeperioder,
            vedtaksdatoFørstegangsbehandling = vedtaksdatoFørstegangsbehandling?.toLocalDate(),
        )
    }
}