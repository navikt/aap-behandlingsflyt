package no.nav.aap.behandlingsflyt.steg.krav

import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

class DatoFraDokumentUtleder(private val mottattDokumentRepository: MottattDokumentRepository) {

    fun utledSøknadsdatoForSak(sakId: SakId): LocalDateTime? {
        val søknader = mottattDokumentRepository.hentDokumenterAvType(
            sakId,
            InnsendingType.SØKNAD
        )
        return søknader
            .minOfOrNull { it.mottattTidspunkt }

    }
}