package no.nav.aap.behandlingsflyt.behandling.behandlerdialog

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.dokumentinnhenting.kontrakt.FastlegeDto
import no.nav.aap.dokumentinnhenting.kontrakt.HentFastlegeDto
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.lookup.repository.RepositoryProvider

class FastlegeService(
    private val dokumentinnhentingGateway: DokumentinnhentingGateway,
    private val sakRepository: SakRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
) {

    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider
    ) : this(
        dokumentinnhentingGateway = gatewayProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    fun utledFastlege(saksnummer: Saksnummer, currentToken: OidcToken): FastlegeResponse {
        val sak = sakRepository.hent(saksnummer)

        val søknad = mottattDokumentRepository
            .hentDokumenterAvType(sak.id, InnsendingType.SØKNAD)
            .maxByOrNull { it.mottattTidspunkt }
            ?.strukturerteData<SøknadV0>()?.data

        val fastlege = dokumentinnhentingGateway.hentFastlege(
            HentFastlegeDto(
                saksnummer = sak.saksnummer.toString(),
                personIdent = sak.person.aktivIdent().identifikator
            ),
            currentToken = currentToken
        )

        return utledFastlegeResponse(fastlege, søknad)
    }

    private fun utledFastlegeResponse(fastlege: FastlegeDto, søknad: SøknadV0?): FastlegeResponse {
        val gjeldendeFastlege = fastlege.fastlege
        val fastlegeFraSøknad = søknad?.fastlege
            ?.find { erSammeBehandler(gjeldendeFastlege, it) }
            ?: søknad?.fastlege?.firstOrNull()

        return FastlegeResponse(
            fastlege = gjeldendeFastlege?.tilDto(),
            erFastlegeEndretSidenSøknadstidspunkt = !erSammeBehandler(gjeldendeFastlege, fastlegeFraSøknad),
            varFastlegeRiktigPåSøknadstidspunkt = fastlegeFraSøknad?.erRegistrertFastlegeRiktig != JaNei.Nei,
            andreBehandlereFraSøknad = søknad?.andreBehandlere?.map { it.tilDto() } ?: emptyList(),
        )
    }

    private fun erSammeBehandler(
        gjeldendeFastlege: no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto?,
        fastlegeFraSøknad: no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FastlegeDto?
    ): Boolean {
        return gjeldendeFastlege?.behandlerRef != null && gjeldendeFastlege.behandlerRef == fastlegeFraSøknad?.behandlerRef
    }

    private fun no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto.tilDto(): BehandlerDto {
        return BehandlerDto(
            behandlerRef = behandlerRef,
            hprId = hprId,
            navn = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" "),
            kontor = kontor,
            adresse = adresse,
            postnummer = postnummer,
            poststed = poststed,
            telefon = telefon,
        )
    }

    private fun no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlerDto.tilDto(): ManueltOppgittBehandlerDto {
        return ManueltOppgittBehandlerDto(
            navn = listOfNotNull(firstname, lastname).joinToString(" "),
            legekontor = legekontor,
            adresse = gateadresse,
            postnummer = postnummer,
            poststed = poststed,
            telefon = telefon,
        )
    }
}

data class FastlegeResponse(
    val fastlege: BehandlerDto?,
    val erFastlegeEndretSidenSøknadstidspunkt: Boolean,
    val varFastlegeRiktigPåSøknadstidspunkt: Boolean,
    val andreBehandlereFraSøknad: List<ManueltOppgittBehandlerDto>
)

data class BehandlerDto(
    val behandlerRef: String,
    val hprId: String?,
    val navn: String,
    val kontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)

data class ManueltOppgittBehandlerDto(
    val navn: String?,
    val legekontor: String?,
    val adresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val telefon: String?,
)
