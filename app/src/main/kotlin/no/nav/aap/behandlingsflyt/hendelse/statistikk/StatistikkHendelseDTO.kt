package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

data class StatistikkHendelseDTO(val saksnummer: String, val status: Status, val behandlingType: TypeBehandling)