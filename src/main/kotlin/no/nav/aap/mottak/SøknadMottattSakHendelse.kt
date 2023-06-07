package no.nav.aap.mottak

import no.nav.aap.domene.typer.Saksnummer

class SøknadMottattSakHendelse(private val saksnummer: Saksnummer) : SakHendelse {
    override fun saksnummer(): Saksnummer {
        return saksnummer
    }

    override fun tilBehandlingHendelse(behandlingId: Long): BehandlingHendelse {
        return SøknadMottattBehandlingHendelse(behandlingId)
    }
}
