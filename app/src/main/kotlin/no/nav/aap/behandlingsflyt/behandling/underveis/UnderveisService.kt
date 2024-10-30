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
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Dagsatser
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class UnderveisService(
    private val behandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val pliktkortRepository: PliktkortRepository,
    private val underveisRepository: UnderveisRepository,
    private val aktivitetspliktRepository: AktivitetspliktRepository,
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService
) {

    private val kvoteService = KvoteService()

    private val regelset = listOf(
        RettTilRegel(),
        InstitusjonRegel(),
        SoningRegel(),
        MeldepliktRegel(),
        SammenstiltAktivitetspliktRegel(),
        GraderingArbeidRegel(),
        VarighetRegel(),
    )

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
                        trekk = if (it.verdi.skalReduseresDagsatser()) Dagsatser(it.periode.antallDager()) else Dagsatser(0)
                    )
                },
            input
        )
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        return regelset.fold(Tidslinje()) { resultat, regel ->
            regel.vurder(input, resultat)
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
                    Vilkårtype.BISTANDSVILKÅRET
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

        return UnderveisInput(
            rettighetsperiode = sak.rettighetsperiode,
            relevanteVilkår = relevanteVilkår,
            opptrappingPerioder = listOf(),
            pliktkort = pliktkort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvote = kvote,
            aktivitetspliktGrunnlag = aktivitetspliktGrunnlag,
            etAnnetSted = etAnnetSted,
        )
    }
}