package no.nav.aap.behandlingsflyt.hendelse.mottak

class DokumentMottattSakHendelse() : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
