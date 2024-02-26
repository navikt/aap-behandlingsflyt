package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

interface YrkesskadeRegisterGateway {
    fun innhent(identer: List<Ident>, periode: Periode): List<Periode>
}