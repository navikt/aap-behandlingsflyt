package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.LegeerklæringPurringDto
import no.nav.aap.dokumentinnhenting.kontrakt.MarkerBestillingSomMottattDto
import no.nav.aap.komponenter.gateway.Gateway

interface DokumentinnhentingGateway : Gateway {

    fun bestillLegeerklæring(request: BehandlingsflytToDokumentInnhentingBestillingDto): String
    fun purrPåLegeerklæring(purringRequest: LegeerklæringPurringDto): String
    fun markerDialogmeldingStatusSomMottatt(markerSomMottattRequest: MarkerBestillingSomMottattDto): DialogmeldingStatusTilBehandslingsflytDto
    fun legeerklæringStatus(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDto>
    fun forhåndsvisDialogmelding(request: ForhåndsvisDialogmeldingDto): DialogmeldingForhåndsvisningDto
}