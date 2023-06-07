package no.nav.aap.mottak

import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import no.nav.aap.domene.typer.Saksnummer

interface PersonHendelse {

    fun ident(): Ident

    fun periode(): Periode

    fun tilSakshendelse(saksnummer: Saksnummer): SakHendelse
}
