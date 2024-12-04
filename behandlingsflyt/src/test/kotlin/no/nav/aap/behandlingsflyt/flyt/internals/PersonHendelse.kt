package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.komponenter.type.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
