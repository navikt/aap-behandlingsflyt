package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVBRUTT
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.KVALITETSSIKRET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_BESLUTTER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.TOTRINNS_VURDERT
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType

fun oppdaterAvklaringsbehov(
    avklaringsbehovene: Avklaringsbehovene,
    definisjon: Definisjon,
    vedtakBehøverVurdering: () -> Boolean,
    erTilstrekkeligVurdert: () -> Boolean,
    tilbakestillGrunnlag: () -> Unit,
) {
    require(definisjon.løsesISteg != StegType.UDEFINERT)
    val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)

    if (vedtakBehøverVurdering()) {
        if (avklaringsbehov == null || !avklaringsbehov.harAvsluttetStatusIHistorikken()) {
            when (avklaringsbehov?.status()) {
                OPPRETTET -> {
                    /* ønsket tilstand er OPPRETTET */
                }

                null, AVBRUTT ->
                    avklaringsbehovene.leggTil(listOf(definisjon), definisjon.løsesISteg)

                TOTRINNS_VURDERT,
                SENDT_TILBAKE_FRA_BESLUTTER,
                KVALITETSSIKRET,
                SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                AVSLUTTET ->
                    error("ikke mulig")
            }
        } else if (erTilstrekkeligVurdert()) {
            when (avklaringsbehov.status()) {
                OPPRETTET, AVBRUTT ->
                    avklaringsbehovene.avslutt(definisjon)

                AVSLUTTET,
                SENDT_TILBAKE_FRA_BESLUTTER,
                KVALITETSSIKRET,
                SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                TOTRINNS_VURDERT -> {
                    /* uendret status */
                }
            }
        } else {
            when (avklaringsbehov.status()) {
                OPPRETTET -> {
                    /* forbli OPPRETTET */
                }

                AVSLUTTET,
                TOTRINNS_VURDERT,
                SENDT_TILBAKE_FRA_BESLUTTER,
                KVALITETSSIKRET,
                SENDT_TILBAKE_FRA_KVALITETSSIKRER,
                AVBRUTT -> {
                    avklaringsbehovene.leggTil(listOf(definisjon), definisjon.løsesISteg)
                }
            }
        }
    } else {
        when (avklaringsbehov?.status()) {
            null,
            AVBRUTT -> {
                /* trenger ikke menneskelig vurdering for vedtaket. */
            }

            OPPRETTET,
            AVSLUTTET,
            TOTRINNS_VURDERT,
            SENDT_TILBAKE_FRA_BESLUTTER,
            KVALITETSSIKRET,
            SENDT_TILBAKE_FRA_KVALITETSSIKRER -> {
                avklaringsbehovene.avbryt(definisjon)
            }
        }

        if (avklaringsbehov?.harAvsluttetStatusIHistorikken() == true) {
            tilbakestillGrunnlag()
        }
    }
}
