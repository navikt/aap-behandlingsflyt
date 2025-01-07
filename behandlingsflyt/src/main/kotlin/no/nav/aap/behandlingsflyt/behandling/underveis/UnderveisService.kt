package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.InstitusjonRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MapInstitusjonoppholdTilRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.RettTilRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SammenstiltAktivitetspliktRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.SoningRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Dagsatser
import kotlin.reflect.KClass

class UnderveisService(
    private val behandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val pliktkortRepository: PliktkortRepository,
    private val underveisRepository: UnderveisRepository,
    private val aktivitetspliktRepository: AktivitetspliktRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val meldepliktRepository: MeldepliktRepository,
) {

    private val kvoteService = KvoteService()

    companion object {
        private val regelset = listOf(
            RettTilRegel(),
            UtledMeldeperiodeRegel(),
            InstitusjonRegel(),
            SoningRegel(),
            MeldepliktRegel(),
            SammenstiltAktivitetspliktRegel(),
            GraderingArbeidRegel(),
            VarighetRegel(),
        )

        init {
            fun checkAvhengighet(forventetFør: KClass<*>, forventetEtter: KClass<*>) {
                val offset1 = regelset.indexOfFirst { it::class == forventetFør }
                val offset2 = regelset.indexOfFirst { it::class == forventetEtter }
                check(offset1 != -1) { "Regel ${forventetFør.qualifiedName} er ikke med" }
                check(offset2 != -1) { "Regel ${forventetEtter.qualifiedName} er ikke med" }
                check(offset1 < offset2) {
                    "Regel ${forventetFør.qualifiedName} må ha kjørt før ${forventetEtter.qualifiedName}, men er kjørt etter"
                }
            }

            checkAvhengighet(forventetFør = UtledMeldeperiodeRegel::class, forventetEtter = MeldepliktRegel::class)
            checkAvhengighet(
                forventetFør = UtledMeldeperiodeRegel::class,
                forventetEtter = SammenstiltAktivitetspliktRegel::class
            )
            checkAvhengighet(forventetFør = UtledMeldeperiodeRegel::class, forventetEtter = GraderingArbeidRegel::class)
        }
    }

    fun vurder(behandlingId: BehandlingId): Tidslinje<Vurdering> {
        val input = genererInput(behandlingId)

        val vurderRegler = vurderRegler(input)
        underveisRepository.lagre(
            behandlingId,
            vurderRegler.segmenter()
                .map {
                    Underveisperiode(
                        periode = it.periode,
                        meldePeriode = it.verdi.meldeperiode(),
                        utfall = it.verdi.utfall(),
                        avslagsårsak = it.verdi.avslagsårsak(),
                        grenseverdi = it.verdi.grenseverdi(),
                        gradering = it.verdi.gradering(),
                        trekk = if (it.verdi.skalReduseresDagsatser()) Dagsatser(1) else Dagsatser(0),
                        brukerAvKvoter = it.verdi.varighetVurdering?.brukerAvKvoter.orEmpty(),
                        bruddAktivitetspliktId = it.verdi.aktivitetspliktVurdering?.dokument?.metadata?.id
                    )
                },
            input
        )
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        return regelset.fold(Tidslinje()) { resultat, regel ->
            regel.vurder(input, resultat).kryss(input.rettighetsperiode)
        }
    }

    private fun genererInput(behandlingId: BehandlingId): UnderveisInput {
        val sak = behandlingService.hentSakFor(behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val relevanteVilkår = vilkårsresultat
            .alle()
            .filter { v ->
                v.type in setOf(
                    Vilkårtype.ALDERSVILKÅRET,
                    Vilkårtype.MEDLEMSKAP,
                    Vilkårtype.SYKDOMSVILKÅRET,
                    Vilkårtype.BISTANDSVILKÅRET,
                    Vilkårtype.SYKEPENGEERSTATNING,
                )
            }

        val pliktkortGrunnlag = pliktkortRepository.hentHvisEksisterer(behandlingId)
        val pliktkort = pliktkortGrunnlag?.pliktkort() ?: listOf()
        val innsendingsTidspunkt = pliktkortGrunnlag?.innsendingsdatoPerMelding() ?: mapOf()
        val kvote = kvoteService.beregn(behandlingId)
        val utlederResultat = etAnnetStedUtlederService.utled(behandlingId)

        val etAnnetSted = MapInstitusjonoppholdTilRegel.map(utlederResultat)

        val aktivitetspliktGrunnlag = aktivitetspliktRepository.hentGrunnlagHvisEksisterer(behandlingId)
            ?: AktivitetspliktGrunnlag(bruddene = setOf())

        val arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandlingId)
            ?: ArbeidsevneGrunnlag(vurderinger = emptyList())

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandlingId)
            ?: MeldepliktGrunnlag(vurderinger = emptyList())

        return UnderveisInput(
            rettighetsperiode = sak.rettighetsperiode,
            relevanteVilkår = relevanteVilkår,
            opptrappingPerioder = listOf(),
            pliktkort = pliktkort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvoter = kvote,
            aktivitetspliktGrunnlag = aktivitetspliktGrunnlag,
            etAnnetSted = etAnnetSted,
            arbeidsevneGrunnlag = arbeidsevneGrunnlag,
            meldepliktGrunnlag = meldepliktGrunnlag,
        )
    }
}