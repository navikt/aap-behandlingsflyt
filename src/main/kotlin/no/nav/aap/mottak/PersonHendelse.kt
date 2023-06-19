package no.nav.aap.mottak

import no.nav.aap.domene.typer.Periode

interface PersonHendelse {

    fun periode(): Periode

    fun tilSakshendelse(): SakHendelse
}
