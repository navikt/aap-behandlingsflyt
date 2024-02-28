package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.PliktkortRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.underveis.regler.AktivitetRegel
import no.nav.aap.behandlingsflyt.underveis.regler.EtAnnetStedRegel
import no.nav.aap.behandlingsflyt.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.underveis.regler.RettTilRegel
import no.nav.aap.behandlingsflyt.underveis.regler.SamordningRegel
import no.nav.aap.behandlingsflyt.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.underveis.regler.Vurdering
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class UnderveisService(
    private val behandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val pliktkortRepository: PliktkortRepository,
    private val underveisRepository: UnderveisRepository
) {

    private val kvoteService = KvoteService()

    private val regelset = listOf(
        RettTilRegel(),
        EtAnnetStedRegel(),
        AktivitetRegel(),
        GraderingArbeidRegel(),
        SamordningRegel(),
        VarighetRegel()
    )

    fun vurder(behandlingId: BehandlingId): Tidslinje<Vurdering> {
        val input = genererInput(behandlingId)

        val vurderRegler = vurderRegler(input)
        underveisRepository.lagre(
            behandlingId,
            vurderRegler.segmenter()
                .map {
                    Underveisperiode(
                        it.periode,
                        it.verdi.utfall(),
                        it.verdi.avslagsårsak(),
                        it.verdi.grenseverdi(),
                        it.verdi.gradering()
                    )
                })
        return vurderRegler
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        var resultat: Tidslinje<Vurdering> = Tidslinje()
        regelset.forEach { regel ->
            resultat = regel.vurder(input, resultat)
        }
        return resultat
    }

    private fun genererInput(behandlingId: BehandlingId): UnderveisInput {
        val sak = behandlingService.hentSakFor(behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val relevanteVilkår = vilkårsresultat
            .alle()
            .filter { v ->
                v.type in setOf( // TODO: add medlemskap
                    Vilkårtype.ALDERSVILKÅRET,
                    Vilkårtype.SYKDOMSVILKÅRET,
                    Vilkårtype.BISTANDSVILKÅRET
                )
            }

        val pliktkortGrunnlag = pliktkortRepository.hentHvisEksisterer(behandlingId)
        val pliktkort = pliktkortGrunnlag?.pliktkort() ?: listOf()
        val innsendingsTidspunkt = pliktkortGrunnlag?.innsendingsdatoPerMelding() ?: mapOf()
        val kvote = kvoteService.beregn(behandlingId)

        return UnderveisInput(
            rettighetsperiode = sak.rettighetsperiode,
            relevanteVilkår = relevanteVilkår,
            opptrappingPerioder = listOf(),
            pliktkort = pliktkort,
            innsendingsTidspunkt = innsendingsTidspunkt,
            kvote = kvote
        )
    }
}