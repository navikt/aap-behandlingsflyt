package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingHendelseId
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

public sealed interface TilbakekrevingHendelse : Melding

// https://github.com/navikt/familie-tilbake/blob/main/kontrakter-ekstern-v2/src/main/kotlin/no/nav/tilbakekreving/api/v2/fagsystem/BehandlingEndretHendelse.kt
@JsonIgnoreProperties(ignoreUnknown = true)
public data class TilbakekrevingHendelseKafkaMelding(
    val hendelsestype: String,
    val versjon: Int,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingKafkaDto,
) {

    public fun tilTilbakekrevingHendelseV0(): TilbakekrevingHendelse =
        TilbakekrevingHendelseV0(
            hendelsestype = hendelsestype,
            versjon = versjon,
            eksternFagsakId = eksternFagsakId,
            hendelseOpprettet = hendelseOpprettet,
            eksternBehandlingId = eksternBehandlingId,
            tilbakekreving = tilbakekreving,
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(TilbakekrevingHendelseId.ny(meldingKey)),
            type = InnsendingType.TILBAKEKREVING_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilTilbakekrevingHendelseV0()
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class TilbakekrevingHendelseV0(
    val hendelsestype: String,
    val versjon: Int,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val tilbakekreving: TilbakekrevingKafkaDto,
) : TilbakekrevingHendelse

data class TilbakekrevingKafkaDto(
    val behandlingId: UUID,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: TilbakekrevingPeriode,
)

public enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    AVSLUTTET,
}

public data class TilbakekrevingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)