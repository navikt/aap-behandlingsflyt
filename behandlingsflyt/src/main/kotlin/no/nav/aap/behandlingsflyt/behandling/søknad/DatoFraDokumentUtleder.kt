package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDateTime

class DatoFraDokumentUtleder(private val mottattDokumentRepository: MottattDokumentRepository) {

    fun utledSøknadsdatoForSak(sakId: SakId): LocalDateTime? {
        val søknader = mottattDokumentRepository.hentDokumenterAvType(
            sakId,
            InnsendingType.SØKNAD
        )
        val legeerklæringer = mottattDokumentRepository.hentDokumenterAvType(
            sakId,
            InnsendingType.LEGEERKLÆRING
        )
        return (søknader + legeerklæringer)
            .minOfOrNull { it.mottattTidspunkt }

    }
}