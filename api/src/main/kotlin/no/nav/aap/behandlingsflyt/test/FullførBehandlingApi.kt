package no.nav.aap.behandlingsflyt.test

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import no.nav.aap.behandlingsflyt.Tags
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.behandlingsflyt.utils.withMdc
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.repository.RepositoryRegistry
import javax.sql.DataSource
import java.time.LocalDate
import kotlin.concurrent.thread

fun NormalOpenAPIRoute.fullførBehandlingApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val service = TestBehandlingFullføringService(dataSource, repositoryRegistry, gatewayProvider)
    if (Miljø.erProd()) return
    route("/api/test/opprettOgFullfoerBehandling").tag(Tags.Dolly) {
        @Suppress("UnauthorizedPost")
        post<Unit, OpprettOgFullforBehandlingRespons, OpprettOgFullforBehandlingRequest> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            try {
                val resultat = dataSource.transaction { connection ->
                    TestSakService(repositoryRegistry.provider(connection), gatewayProvider)
                        .opprettTestSak(
                            ident = Ident(req.ident),
                            erStudent = req.erStudent,
                            harYrkesskade = req.harYrkesskade,
                            harMedlemskap = req.harMedlemskap,
                            andreUtbetalinger = req.andreUtbetalinger?.tilKontrakt(),
                            søknadsdato = req.soeknadsdato,
                        )
                }
                thread(isDaemon = true, block = withMdc { service.fullførBehandling(resultat.sak, resultat.ventPåNyBehandling) })
                respond(OpprettOgFullforBehandlingRespons(resultat.sak.saksnummer.toString()))
            } catch (e: OpprettTestSakException) {
                throw UgyldigForespørselException(message = e.message ?: "Ukjent feil", cause = e)
            }
        }
    }

    route("/api/test/behandlingStatus").tag(Tags.Dolly) {
        @Suppress("UnauthorizedPost")
        post<Unit, BehandlingStatusRespons, BehandlingStatusRequest> { _, req ->
            require(!Miljø.erProd()) { "Ikke tilgjengelig i produksjonsmiljøet" }
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val provider = repositoryRegistry.provider(connection)
                val person = provider.provide<PersonRepository>().finn(Ident(req.ident))
                    ?: return@transaction BehandlingStatusRespons(req.ident, null, false)
                val sak = provider.provide<SakRepository>().finnSakerFor(person).firstOrNull()
                    ?: return@transaction BehandlingStatusRespons(req.ident, null, false)
                val behandling = BehandlingService(provider, gatewayProvider)
                    .finnSisteYtelsesbehandlingFor(sak.id)
                    ?: return@transaction BehandlingStatusRespons(sak.saksnummer.toString(), null, false)
                val status = behandling.status()

                val søknad = provider.provide<MottattDokumentRepository>()
                    .hentDokumenterAvType(behandling.id, InnsendingType.SØKNAD)
                    .firstOrNull()
                    ?.strukturerteData<SøknadV0>()
                    ?.data

                val soeknadDetaljer = søknad?.let {
                    SoeknadDetaljer(
                        erStudent = it.student?.erStudent == StudentStatus.Ja,
                        harYrkesskade = it.yrkesskade.equals("Ja", ignoreCase = true),
                        harMedlemskap = it.medlemskap?.harBoddINorgeSiste5År.equals("JA", ignoreCase = true),
                        andreUtbetalinger = it.andreUtbetalinger?.let { a -> AndreUtbetalingerApiDto.fraKontrakt(a) },
                    )
                }

                BehandlingStatusRespons(
                    saksnummer = sak.saksnummer.toString(),
                    behandlingStatus = BehandlingStatusEnum.fraKontrakt(status),
                    ferdig = status == Status.AVSLUTTET,
                    soeknad = soeknadDetaljer,
                )
            }
            respond(respons)
        }
    }
}

data class OpprettOgFullforBehandlingRespons(val saksnummer: String)

data class OpprettOgFullforBehandlingRequest(
    @property:Description("Dolly-ident for test-personen.")
    val ident: String,
    @property:Description("Om personen svarer at han/hun er student i søknaden.")
    val erStudent: Boolean,
    @property:Description("Om personen svarer at han/hun har yrkesskade i søknaden. Urelatert til om det finnes yrkesskade i yrkesskaderegisteret.")
    val harYrkesskade: Boolean,
    @property:Description("Om personen svarer at han/hun har bodd/jobbet i Norge i siste 5 år.")
    val harMedlemskap: Boolean,
    @property:Description("Om søker svarte at hen mottar andre utbetalinger i søknaden.")
    val andreUtbetalinger: AndreUtbetalingerApiDto?,
    @property:Description("Søknadsdato. Brukes som rettighetsperiode.fom og mottattTidspunkt. Defaulter til dagens dato.")
    val soeknadsdato: LocalDate? = null,
)

data class BehandlingStatusRequest(val ident: String)

data class BehandlingStatusRespons(
    @property:Description("Det opprettede saksnummeret.")
    val saksnummer: String,
    @property:Description("Sier om behandlingen er ferdigbehandlet. Om denne ikke er AVSLUTTET innen 1 min, er det antakelig en feil.")
    val behandlingStatus: BehandlingStatusEnum?,
    @property:Description("Om behandlingen er ferdigbehandlet.")
    val ferdig: Boolean,
    val soeknad: SoeknadDetaljer? = null,
)

enum class BehandlingStatusEnum {
    OPPRETTET,
    UTREDES,
    IVERKSETTES,
    AVSLUTTET;

    companion object {
        fun fraKontrakt(status: Status): BehandlingStatusEnum = when (status) {
            Status.OPPRETTET -> OPPRETTET
            Status.UTREDES -> UTREDES
            Status.IVERKSETTES -> IVERKSETTES
            Status.AVSLUTTET -> AVSLUTTET
        }
    }
}

data class SoeknadDetaljer(
    val erStudent: Boolean,
    val harYrkesskade: Boolean, // dårlig navn,
    val harMedlemskap: Boolean,
    val andreUtbetalinger: AndreUtbetalingerApiDto?,
)