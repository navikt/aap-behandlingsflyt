package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingFagsysteminfoBehovHendelseId
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
    val tilbakekreving: TilbakekrevingKafkaDto? = null,
    val sakOpprettet: LocalDateTime? = null,
    val varselSendt: LocalDate? = null,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus? = null,
    val totaltFeilutbetaltBeløp: BigDecimal? = null,
    val saksbehandlingURL: String? = null,
    val fullstendigPeriode: TilbakekrevingPeriode? = null,

    ) {

    public fun tilTilbakekrevingHendelseV0(): TilbakekrevingHendelse {

        //TODO: triks for å støtte både ny og gammel datastruktur. Kan slettes når alle gamle meldinger er ferdig.
        val nyTilbakekreving = tilbakekreving
            ?: TilbakekrevingKafkaDto(
                    //TODO: Litt stygg hack for å hente behandlingId fra url ettersom den ikke er med i gammel datastruktur.
                    behandlingId = UUID.fromString(saksbehandlingURL!!.substringAfterLast('/')),
                    sakOpprettet = sakOpprettet!!,
                    varselSendt = varselSendt,
                    behandlingsstatus = behandlingsstatus!!,
                    totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp!!,
                    saksbehandlingURL = saksbehandlingURL,
                    fullstendigPeriode = fullstendigPeriode!!
            )


        return TilbakekrevingHendelseV0(
            hendelsestype = hendelsestype,
            versjon = versjon,
            eksternFagsakId = eksternFagsakId,
            hendelseOpprettet = hendelseOpprettet,
            eksternBehandlingId = eksternBehandlingId,
            tilbakekreving = nyTilbakekreving,
        )
    }

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


public data class FagsysteminfoBehovKafkaMelding(
    val hendelsestype: String,
    val versjon: Int,
    val eksternFagsakId: String,
    val kravgrunnlagReferanse: String,
    val hendelseOpprettet: LocalDateTime,
) {

    public fun tilFagsysteminfoBehovV0(): FagsysteminfoBehovV0 =
        FagsysteminfoBehovV0(
            hendelsestype = hendelsestype,
            versjon = versjon,
            eksternFagsakId = eksternFagsakId,
            kravgrunnlagReferanse = kravgrunnlagReferanse,
            hendelseOpprettet = hendelseOpprettet,
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(TilbakekrevingFagsysteminfoBehovHendelseId.ny(meldingKey)),
            type = InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilFagsysteminfoBehovV0(),
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


@JsonIgnoreProperties(ignoreUnknown = true)
public data class FagsysteminfoBehovV0(
    val hendelsestype: String,
    val versjon: Int,
    val eksternFagsakId: String,
    val kravgrunnlagReferanse: String,
    val hendelseOpprettet: LocalDateTime,
) : TilbakekrevingHendelse

public data class TilbakekrevingKafkaDto(
    val behandlingId: UUID,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: TilbakekrevingPeriode,
)

public enum class TilbakekrevingBehandlingsstatus {
    OPPRETTET,
    TIL_BEHANDLING,
    TIL_BESLUTTER,
    RETUR_FRA_BESLUTTER,
    AVSLUTTET,
}

public data class TilbakekrevingPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)