package no.nav.aap.behandlingsflyt.behandling.svarfraandreinstans.svarfraandreinstans

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AnkeUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlingEventType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KabalHendelseV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OmgjoeringsUtfall
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TrygderettUtfall

data class SvarFraAnderinstansGrunnlagDto(
    val svarFraAndreinstans: SvarFraAndreinstansDto
)

data class SvarFraAndreinstansDto(
    val type: BehandlingEventType,
    val utfall: Utfall?,
    val feilregistrertBegrunnelse: String?
)

enum class Utfall {
    TRUKKET,
    RETUR,
    OPPHEVET,
    MEDHOLD,
    DELVIS_MEDHOLD,
    STADFESTELSE,
    UGUNST,
    AVVIST,
    HEVET,
    INNSTILLING_STADFESTELSE,
    INNSTILLING_AVVIST,
    MEDHOLD_ETTER_FVL_35;

    companion object {
        fun fraHendelse(hendelse: KabalHendelseV0): Utfall? {
            return when (hendelse.type) {
                BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET ->
                    fraKlageUtfall(hendelse.detaljer.klagebehandlingAvsluttet?.utfall!!)

                BehandlingEventType.ANKEBEHANDLING_AVSLUTTET ->
                    fraAnkeUtfall(hendelse.detaljer.ankebehandlingAvsluttet?.utfall!!)

                BehandlingEventType.ANKE_I_TRYGDERETTENBEHANDLING_OPPRETTET
                    -> fraTrygderettUtfall(hendelse.detaljer.ankeITrygderettenbehandlingOpprettet?.utfall!!)

                BehandlingEventType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET_AVSLUTTET
                    -> fraKlageUtfall(hendelse.detaljer.behandlingEtterTrygderettenOpphevetAvsluttet?.utfall!!)

                BehandlingEventType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET
                    -> fraOmgjøringsUtfall(hendelse.detaljer.omgjoeringskravbehandlingAvsluttet?.utfall!!)

                else -> null
            }
        }


        fun fraKlageUtfall(klageUtfall: KlageUtfall): Utfall {
            return when (klageUtfall) {
                KlageUtfall.TRUKKET -> TRUKKET
                KlageUtfall.RETUR -> RETUR
                KlageUtfall.OPPHEVET -> OPPHEVET
                KlageUtfall.MEDHOLD -> MEDHOLD
                KlageUtfall.DELVIS_MEDHOLD -> DELVIS_MEDHOLD
                KlageUtfall.STADFESTELSE -> STADFESTELSE
                KlageUtfall.UGUNST -> UGUNST
                KlageUtfall.AVVIST -> AVVIST
            }
        }

        fun fraAnkeUtfall(ankeUtfall: AnkeUtfall): Utfall {
            return when (ankeUtfall) {
                AnkeUtfall.TRUKKET -> TRUKKET
                AnkeUtfall.RETUR -> RETUR
                AnkeUtfall.OPPHEVET -> OPPHEVET
                AnkeUtfall.MEDHOLD -> MEDHOLD
                AnkeUtfall.DELVIS_MEDHOLD -> DELVIS_MEDHOLD
                AnkeUtfall.STADFESTELSE -> STADFESTELSE
                AnkeUtfall.UGUNST -> UGUNST
                AnkeUtfall.AVVIST -> AVVIST
                AnkeUtfall.HEVET -> HEVET
            }
        }

        fun fraTrygderettUtfall(trygderettUtfall: TrygderettUtfall): Utfall {
            return when (trygderettUtfall) {
                TrygderettUtfall.TRUKKET -> TRUKKET
                TrygderettUtfall.OPPHEVET -> OPPHEVET
                TrygderettUtfall.MEDHOLD -> MEDHOLD
                TrygderettUtfall.DELVIS_MEDHOLD -> DELVIS_MEDHOLD
                TrygderettUtfall.INNSTILLING_STADFESTELSE -> INNSTILLING_STADFESTELSE
                TrygderettUtfall.INNSTILLING_AVVIST -> INNSTILLING_AVVIST
            }
        }

        fun fraOmgjøringsUtfall(omgjøringsUtfall: OmgjoeringsUtfall): Utfall {
            return when (omgjøringsUtfall) {
                OmgjoeringsUtfall.MEDHOLD_ETTER_FVL_35 -> MEDHOLD_ETTER_FVL_35
            }
        }
    }
}

internal fun KabalHendelseV0.tilDto(): SvarFraAndreinstansDto {
    return SvarFraAndreinstansDto(
        type = this.type,
        utfall = Utfall.fraHendelse(this),
        feilregistrertBegrunnelse = this.detaljer.behandlingFeilregistrert?.reason
    )
}