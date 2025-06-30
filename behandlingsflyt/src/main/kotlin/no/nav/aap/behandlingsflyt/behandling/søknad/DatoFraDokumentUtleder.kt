package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime

class DatoFraDokumentUtleder(private val mottattDokumentRepository: MottattDokumentRepository) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        mottattDokumentRepository = repositoryProvider.provide()
    )

    fun utledSøknadsdatoForSak(sakId: SakId): LocalDateTime? {
        val søknader = mottattDokumentRepository.hentDokumenterAvType(
            sakId,
            InnsendingType.SØKNAD
        )
        return søknader
            .minOfOrNull { it.mottattTidspunkt }

    }

    fun utledKravMottattDatoForKlageBehandling(behandlingId: BehandlingId): LocalDate? {
        val klageDokumenter =
            mottattDokumentRepository.hentDokumenterAvType(behandlingId = behandlingId, InnsendingType.KLAGE)
        val førsteKlageDokument = klageDokumenter.minByOrNull { it.mottattTidspunkt }
        return førsteKlageDokument?.strukturerteData<KlageV0>()?.data?.kravMottatt
    }
}