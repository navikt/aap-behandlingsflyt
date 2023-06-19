package no.nav.aap.mottak

class DokumentMottattSakHendelse() : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
