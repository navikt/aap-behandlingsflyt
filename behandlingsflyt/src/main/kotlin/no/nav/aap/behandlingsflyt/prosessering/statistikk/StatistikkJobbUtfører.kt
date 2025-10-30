package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDateTime


class StatistikkJobbUtfører(
    private val statistikkGateway: StatistikkGateway,
    private val statistikkMetoder: StatistikkMetoder,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)
    override fun utfør(input: JobbInput) {

        log.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload<BehandlingFlytStoppetHendelseTilStatistikk>()

        val oversatt = statistikkMetoder.oversettHendelseTilKontrakt(payload)

        håndterBehandlingStoppet(oversatt)
    }


    private fun håndterBehandlingStoppet(hendelse: StoppetBehandling) {
        statistikkGateway.avgiStatistikk(hendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return StatistikkJobbUtfører(
                statistikkGateway = gatewayProvider.provide(),
                statistikkMetoder = StatistikkMetoder(repositoryProvider)
            )
        }

        override val type = "flyt.statistikk"
        override val navn = "Lagrer statistikk"
        override val beskrivelse = "Skal ta i mot data fra steg i en behandling og sender til statistikk-appen."
    }
}

/**
 * @param status Status på behandlingen.
 * @param opprettetTidspunkt Når behandlingen ble opprettet.
 * @param hendelsesTidspunkt Når denne hendelsen ble opprettet i Behandlingsflyt.
 * @param opprettetAv Hvem som opprettet behandlingen, dersom behandlingen er en manuell revurdering.
 */
data class BehandlingFlytStoppetHendelseTilStatistikk(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val referanse: BehandlingReferanse,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
    val versjon: String,
    val opprettetAv: String? = null,
)
