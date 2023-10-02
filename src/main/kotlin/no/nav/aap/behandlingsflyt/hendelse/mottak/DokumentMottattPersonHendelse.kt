package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.domene.Periode

class DokumentMottattPersonHendelse(private val periode: no.nav.aap.behandlingsflyt.domene.Periode) : PersonHendelse {

    override fun periode(): no.nav.aap.behandlingsflyt.domene.Periode {
        return periode
    }

    override fun tilSakshendelse(): SakHendelse {
        return DokumentMottattSakHendelse()
    }
}
