package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.util.UUID

class KlagedokumentInformasjonUtleder(private val mottattDokumentRepository: MottattDokumentRepository) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        repositoryProvider.provide()
    )

    fun utledKravMottattDatoForKlageBehandling(behandlingId: BehandlingId): LocalDate? {
        val klageDokumenter =
            mottattDokumentRepository.hentDokumenterAvType(behandlingId = behandlingId, InnsendingType.KLAGE)
        val førsteKlageDokument = klageDokumenter.minByOrNull { it.mottattTidspunkt }
        return førsteKlageDokument?.strukturerteData<KlageV0>()?.data?.kravMottatt
    }

    fun utledKlagebehandlingForSvar(svarFraKabalBehandlingId: BehandlingId): BehandlingReferanse? {
        val kabalHendelseDokumenter = mottattDokumentRepository.hentDokumenterAvType(
            behandlingId = svarFraKabalBehandlingId,
            InnsendingType.KABAL_HENDELSE
        )

        val tilhørendeKlagebehandling =
            kabalHendelseDokumenter.firstOrNull()?.strukturerteData<KabalHendelseV0>()?.data?.kildeReferanse
                ?: throw IllegalStateException(
                    "Fant ikke tilhørende klagebehandling for behandling $svarFraKabalBehandlingId"
                )

        return BehandlingReferanse(
            UUID.fromString(tilhørendeKlagebehandling)
        )
    }
}