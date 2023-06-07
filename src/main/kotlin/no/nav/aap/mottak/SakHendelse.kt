package no.nav.aap.mottak

import no.nav.aap.domene.typer.Saksnummer

interface SakHendelse {

    fun saksnummer(): Saksnummer

    fun tilBehandlingHendelse(behandlingId: Long): BehandlingHendelse
}
