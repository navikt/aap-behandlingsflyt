package no.nav.aap.behandlingsflyt.prosessering.tilbakekreving

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class SendFagsysteminfoBehovTilTilbakekrevingUtfører(
    val sakRepository: SakRepository,
): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<FagsysteminfoSvarHendelse>()
        val produsent = TilbakekrevingFagsystemInfoProdusent(KafkaProducerConfig())
        val sak = sakRepository.hent(SakId(input.sakId()))
        val fnr = sak.person.aktivIdent().identifikator
        log.info("Send fagsysteminfo til tilbakekreving for sak ${sak.saksnummer} ")
        produsent.sendFagsystemInfo(fnr, hendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {

            return SendFagsysteminfoBehovTilTilbakekrevingUtfører(
                sakRepository = repositoryProvider.provide(),
            )
        }

        override val beskrivelse = "Send fagsysteminfo til tilbakekreving"
        override val navn = "Send fagsysteminfo til tilbakekreving"
        override val type = "flyt.sendFagsysteminfoHendelse"
    }

}

data class FagsysteminfoSvarHendelse(
    val hendelsestype: String = "fagsysteminfo_svar",
    val versjon: Int = 1,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val mottaker: MottakerDto,
    val revurdering: RevurderingDto,
    val utvidPerioder: List<UtvidetPeriodeDto>?,
    val behandlendeEnhet: String?,
) {
    data class UtvidetPeriodeDto(
        val kravgrunnlagPeriode: PeriodeDto,
        val vedtaksperiode: PeriodeDto,
    )

    data class RevurderingDto(
        val behandlingId: String,
        val årsak: Årsak,
        val årsakTilFeilutbetaling: String?,
        val vedtaksdato: LocalDate,
    ) {
        enum class Årsak {
            NYE_OPPLYSNINGER,
            KORRIGERING,
            KLAGE,
            UKJENT,
        }
    }

}

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class MottakerDto(
    val ident: String,
    val type: MottakerType,
) {
    enum class MottakerType {
        PERSON,
    }
}

