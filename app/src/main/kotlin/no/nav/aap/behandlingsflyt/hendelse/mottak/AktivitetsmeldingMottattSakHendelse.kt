package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.flyt.flate.HammerDto
import java.time.LocalDateTime

class AktivitetsmeldingMottattSakHendelse(
    val mottattTidspunkt: LocalDateTime,
    val hammer: HammerDto
) : SakHendelse {

    override fun tilBehandlingHendelse(): BehandlingHendelse {
        return DokumentMottattBehandlingHendelse()
    }
}
