package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.sak.Ident
import java.time.Year

object InntektRegisterMock {
    private val inntekter = HashMap<Ident, List<InntektPerÅr>>()

    fun innhent(identer: List<Ident>, år: Set<Year>): Set<InntektPerÅr> {
        val resultat: MutableSet<InntektPerÅr> = mutableSetOf()
        for (year in år) {
            val relevanteIdenter = inntekter.filter { entry -> identer.contains(entry.key) }
            val summerteInntekter = relevanteIdenter
                .flatMap { it.value }
                .filter { it.år == year }
                .sumOf { it.beløp.verdi() }

            resultat.add(InntektPerÅr(year, Beløp(summerteInntekter)))
        }
        return resultat.toSortedSet()
    }

    fun konstruer(ident: Ident, inntekterPerÅr: List<InntektPerÅr>) {
        inntekter[ident] = inntekterPerÅr
    }
}