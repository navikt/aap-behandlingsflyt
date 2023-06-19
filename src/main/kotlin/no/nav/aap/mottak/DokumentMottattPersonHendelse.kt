package no.nav.aap.mottak

import no.nav.aap.domene.typer.Periode

class DokumentMottattPersonHendelse(private val periode: Periode) : PersonHendelse {

    override fun periode(): Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse()
    }
}
