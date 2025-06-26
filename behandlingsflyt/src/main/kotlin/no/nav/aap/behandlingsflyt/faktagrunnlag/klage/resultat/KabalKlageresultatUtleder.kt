package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType.KABAL_HENDELSE
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling

class KabalKlageresultatUtleder(
    private val mottattDokumentRepository: MottattDokumentRepository,
) {
    fun hentKabalHendelserForKlage(behandling: Behandling): List<KabalHendelseV0> {
        val kabalHendelser = mottattDokumentRepository.hentDokumenterAvType(behandling.sakId, KABAL_HENDELSE)
        return kabalHendelser
            .filter { it.strukturerteData<KabalHendelseV0>()?.data?.kildeReferanse == behandling.referanse.toString() }
            .sortedBy { it.mottattTidspunkt }
            .mapNotNull { it.strukturerteData<KabalHendelseV0>()?.data }
    }
}

