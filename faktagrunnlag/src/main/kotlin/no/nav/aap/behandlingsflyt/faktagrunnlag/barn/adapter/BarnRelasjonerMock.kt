package no.nav.aap.behandlingsflyt.faktagrunnlag.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.Barn
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.verdityper.Periode

object BarnRelasjonerMock {
    fun innhent(identer: List<Ident>, rettighetsperiode: Periode): List<Barn> {
        return listOf()
    }
}