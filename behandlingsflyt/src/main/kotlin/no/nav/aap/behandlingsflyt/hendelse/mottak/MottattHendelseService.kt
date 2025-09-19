package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dokumentHendelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.LoggerFactory

class MottattHendelseService(
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val flytJobbRepository: FlytJobbRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sakRepository = repositoryProvider.provide<SakRepository>(),
        mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>(),
        flytJobbRepository = repositoryProvider.provide<FlytJobbRepository>()
    )

    private val log = LoggerFactory.getLogger(MottattHendelseService::class.java)

    fun registrerMottattHendelse(
        dto: Innsending,
    ) {
        val sak = sakRepository.hent(dto.saksnummer)

        log.info("Mottok dokumenthendelse. Brevkategori: ${dto.type} Mottattdato: ${dto.mottattTidspunkt}")

        if (kjennerTilDokumentFraFør(dto, sak, mottattDokumentRepository)) {
            log.warn("Allerede håndtert dokument med referanse {}", dto.referanse)
        } else {
            prometheus.dokumentHendelse(dto.type).increment()
            flytJobbRepository.leggTil(
                HendelseMottattHåndteringJobbUtfører.nyJobb(
                    sakId = sak.id,
                    dokumentReferanse = dto.referanse,
                    brevkategori = dto.type,
                    kanal = dto.kanal,
                    melding = dto.melding,
                    mottattTidspunkt = dto.mottattTidspunkt
                ),
            )
        }
    }
}

private fun kjennerTilDokumentFraFør(
    innsending: Innsending,
    sak: Sak,
    mottattDokumentRepository: MottattDokumentRepository
): Boolean {
    val innsendinger = mottattDokumentRepository.hentDokumenterAvType(sak.id, innsending.type)

    return innsendinger.any { dokument -> dokument.referanse == innsending.referanse }
}