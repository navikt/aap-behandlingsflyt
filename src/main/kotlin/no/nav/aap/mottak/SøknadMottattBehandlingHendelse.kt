package no.nav.aap.mottak

class SøknadMottattBehandlingHendelse(private val behandlingId: Long) : BehandlingHendelse {
    override fun behandlingId(): Long {
        return behandlingId
    }
}
