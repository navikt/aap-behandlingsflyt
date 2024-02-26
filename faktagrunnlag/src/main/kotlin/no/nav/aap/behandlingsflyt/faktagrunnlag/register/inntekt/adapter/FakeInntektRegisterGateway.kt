package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektRegisterGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.Year

object FakeInntektRegisterGateway : InntektRegisterGateway {
    private val inntekter =
        HashMap<Ident, List<InntektPerÅr>>()

    override fun innhent(
        person: Person,
        år: Set<Year>
    ): Set<InntektPerÅr> {
        val resultat: MutableSet<InntektPerÅr> =
            mutableSetOf()
        for (year in år) {
            val relevanteIdenter = inntekter.filter { entry -> person.identer().contains(entry.key) }
            val summerteInntekter = relevanteIdenter
                .flatMap { it.value }
                .filter { it.år == year }
                .sumOf { it.beløp.verdi() }

            resultat.add(
                InntektPerÅr(
                    year,
                    Beløp(summerteInntekter)
                )
            )
        }
        return resultat.toSortedSet()
    }

    fun konstruer(
        ident: Ident,
        inntekterPerÅr: List<InntektPerÅr>
    ) {
        inntekter[ident] = inntekterPerÅr
    }
}