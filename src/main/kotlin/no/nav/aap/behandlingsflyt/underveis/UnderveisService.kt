package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.underveis.regler.AktivitetRegel
import no.nav.aap.behandlingsflyt.underveis.regler.EtAnnetStedRegel
import no.nav.aap.behandlingsflyt.underveis.regler.GraderingArbeidRegel
import no.nav.aap.behandlingsflyt.underveis.regler.RettTilRegel
import no.nav.aap.behandlingsflyt.underveis.regler.UnderveisInput
import no.nav.aap.behandlingsflyt.underveis.regler.Vurdering
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

class UnderveisService {

    private val regelset = listOf(
        RettTilRegel(),
        EtAnnetStedRegel(),
        AktivitetRegel(),
        GraderingArbeidRegel()
    )

    fun vurder(kontekst: FlytKontekst) {
        val resultat = Tidslinje<Vurdering>(listOf())

        vurderRegler(kontekst, resultat)

    }

    internal fun vurderRegler(
        kontekst: FlytKontekst,
        resultat: Tidslinje<Vurdering>
    ) {
        regelset.forEach { regel ->
            val input = genererInput(kontekst, resultat)
            regel.vurder(input, resultat)
        }
    }

    fun genererInput(kontekst: FlytKontekst, resultat: Tidslinje<Vurdering>): UnderveisInput {
        TODO("GEnerer ")
    }
}