package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.søknad

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.hendelse.mottak.dokument.PeriodisertData

class Søknad(private val periode: Periode) : PeriodisertData {
    override fun periode(): Periode {
        return periode
    }
}