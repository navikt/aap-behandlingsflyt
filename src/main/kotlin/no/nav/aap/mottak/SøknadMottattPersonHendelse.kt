package no.nav.aap.mottak

import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import no.nav.aap.domene.typer.Saksnummer

class SøknadMottattPersonHendelse(private val ident: Ident, private val periode: Periode) : PersonHendelse {
    override fun ident(): Ident {
        return ident
    }

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(saksnummer: Saksnummer): SakHendelse {
        return SøknadMottattSakHendelse(saksnummer)
    }
}
