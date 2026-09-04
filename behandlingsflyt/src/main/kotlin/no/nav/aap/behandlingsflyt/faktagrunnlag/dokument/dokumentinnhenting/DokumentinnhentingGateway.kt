package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.behandling.behandlerdialog.DokumenterForJournalpostParameter
import no.nav.aap.behandlingsflyt.behandling.behandlerdialog.HentDokumentoversiktJournalpostListeResponse
import no.nav.aap.behandlingsflyt.behandling.behandlerdialog.HentDokumentoversiktJournalpostResponse
import no.nav.aap.behandlingsflyt.behandling.dialogmelding.HentDialogmeldingerForSakParams
import no.nav.aap.behandlingsflyt.behandling.dialogmelding.HentDokumentoversiktJournalpostListeParams
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlingsflytToDokumentInnhentingBestillingDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingForhåndsvisningDto
import no.nav.aap.dokumentinnhenting.kontrakt.DialogmeldingStatusTilBehandslingsflytDto
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.FellesDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.ForhåndsvisDialogmeldingDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentFastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.PåminnelseDto
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken

interface DokumentinnhentingGateway : Gateway {
    fun bestillLegeerklæring(request: BehandlingsflytToDokumentInnhentingBestillingDto): String
    fun sendPåminnelseForBestilling(påminnelseRequest: PåminnelseDto): String
    fun avbrytAutomatiskPåminnelseForBestilling(påminnelseRequest: PåminnelseDto)
    fun gjenopptaAutomatiskPåminnelseForBestilling(påminnelseRequest: PåminnelseDto)
    fun legeerklæringStatus(saksnummer: String): List<DialogmeldingStatusTilBehandslingsflytDto>
    fun forhåndsvisDialogmelding(request: ForhåndsvisDialogmeldingDto): DialogmeldingForhåndsvisningDto
    fun hentDialogmeldingerForSak(request: HentDialogmeldingerForSakParams): List<FellesDialogmeldingDto>
    fun hentDokumentoversiktForJournalpost(request: DokumenterForJournalpostParameter): HentDokumentoversiktJournalpostResponse
    fun hentDokumentoversiktForJournalpostListe(request: HentDokumentoversiktJournalpostListeParams): HentDokumentoversiktJournalpostListeResponse
    fun hentFastlege(request: HentFastlegeDto, currentToken: OidcToken): FastlegeDto
}