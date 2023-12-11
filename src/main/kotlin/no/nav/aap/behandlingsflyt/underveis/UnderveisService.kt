package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.underveis.regler.AktivitetRegel
import no.nav.aap.behandlingsflyt.underveis.regler.EtAnnetStedRegel
import no.nav.aap.behandlingsflyt.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.underveis.regler.RettTilRegel
import no.nav.aap.behandlingsflyt.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

class UnderveisService(private val vilkårsresultatRepository: VilkårsresultatRepository) {

    private val regelset = listOf(
        RettTilRegel(),
        EtAnnetStedRegel(),
        AktivitetRegel(),
        GraderingArbeidRegel()
    )

    fun vurder(kontekst: BehandlingId): Tidslinje<Vurdering> {
        val input = genererInput(kontekst)

        return vurderRegler(input)
    }

    internal fun vurderRegler(input: UnderveisInput): Tidslinje<Vurdering> {
        var resultat: Tidslinje<Vurdering> = Tidslinje()
        regelset.forEach { regel ->
            resultat = regel.vurder(input, resultat)
        }
        return resultat
    }

    fun genererInput(behandlingId: BehandlingId): UnderveisInput {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
            .alle()
            .filter { v ->
                v.type in setOf( // TODO: add medlemskap
                    Vilkårtype.ALDERSVILKÅRET,
                    Vilkårtype.SYKDOMSVILKÅRET,
                    Vilkårtype.BISTANDSVILKÅRET
                )
            }
        return UnderveisInput(vilkårsresultat)
    }
}