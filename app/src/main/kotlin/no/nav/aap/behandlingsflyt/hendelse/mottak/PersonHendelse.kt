package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.verdityper.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
