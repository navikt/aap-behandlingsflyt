package no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EierVirksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringVersjon
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryEtableringEgenVirksomRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@Fakes
class EtableringEgenVirksomhetGrunnlagApiTest : BaseApiTest() {
    @Test
    fun `grunnlag teller brukte utviklingsdager og oppstartsdager fra legacy-perioder`() {
        val start = LocalDate.of(2026, 1, 5)
        val (_, forrige, behandling) = opprettInMemorySakOgRevurdering(start)
        val utvikling = Periode(start, start.plusDays(4))
        val oppstart = Periode(start.plusDays(7), start.plusDays(18))
        val legacy = EtableringEgenVirksomhetVurdering(
            begrunnelse = "Historisk vurdering",
            virksomhetNavn = "Testvirksomhet",
            foreliggerFagligVurdering = true,
            virksomhetErNy = true,
            brukerEierVirksomheten = EierVirksomhet.EIER_MINST_50_PROSENT,
            kanFøreTilSelvforsørget = true,
            utviklingsPerioder = listOf(utvikling),
            oppstartsPerioder = listOf(oppstart),
            vurdertAv = Bruker("Z123456"),
            opprettet = Instant.parse("2026-01-05T12:00:00Z"),
            vurdertIBehandling = forrige.id,
            fom = start,
            tom = oppstart.tom,
            fase = null,
            erRegistrertINødvendigeOffentligeRegister = null,
            jobberBrukerAktivMedVirksomheten = null,
            versjon = EtableringVersjon.V0,
        )
        InMemoryEtableringEgenVirksomRepository.lagre(forrige.id, listOf(legacy))

        testApplication {
            installApplication {
                etableringEgenVirksomhetApi(
                    MockDataSource(),
                    inMemoryRepositoryRegistry,
                    createGatewayProvider {
                        register<NomInfoGateway>()
                        register<NorgGateway>()
                    },
                )
            }

            val response = createClient().get(
                "/api/behandling/${behandling.referanse.referanse}/grunnlag/etableringegenvirksomhet"
            ) {
                bearerAuth(AzureTokenGen("behandlingsflyt").generate(false, "behandlingsflyt", "Z123456"))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<EtableringEgenVirksomhetGrunnlagResponse>()
            assertThat(listOf(body.bruktUtviklingsDager, body.bruktOppstartsdager)).containsExactly(5, 10)
        }
    }
}
