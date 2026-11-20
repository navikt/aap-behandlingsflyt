package no.nav.aap.utenlandsopphold

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.StrukturertDokument
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadUtenlandsoppholdV0
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.AzureTokenGen
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySøknadUtenlandsoppholdRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@Fakes
class SøknadUtenlandsoppholdGrunnlagApiTest : BaseApiTest() {

    private val mottattDato = LocalDate.of(2026, 1, 5)
    private val saksbehandler = Bruker("Z123456")

    @Test
    fun `returnerer søknaden uten vurdering og bekreftelse før noe er løst`() {
        val behandling = opprettBehandlingMedSøknad()

        val respons = hentGrunnlag(behandling)

        assertThat(respons.at("/søknad/mottattDato").asText()).isEqualTo(mottattDato.toString())
        assertThat(respons.at("/søknad/journalpostId").asText()).isEqualTo("123")
        assertThat(respons.path("vurdering").isNull).isTrue()
        assertThat(respons.path("bekreftelse").isNull).isTrue()
    }

    @Test
    fun `returnerer vurderingen med vurderingerMeta`() {
        val behandling = opprettBehandlingMedSøknad()
        lagreVurdering(behandling)

        val respons = hentGrunnlag(behandling)

        val vurdering = respons.path("vurdering")
        assertThat(vurdering.path("land").asText()).isEqualTo("SE")
        assertThat(vurdering.path("resultat").asText()).isEqualTo("INNVILGET")
        assertThat(vurdering.path("antallGodkjenteDager").isNull).isTrue()
        assertThat(vurdering.at("/godkjentPeriode/fom").asText()).isEqualTo("2026-02-01")
        assertThat(vurdering.at("/vurderingerMeta/vurdertAv/ident").asText()).isEqualTo(saksbehandler.ident)
        assertThat(vurdering.at("/vurderingerMeta/vurdertAv/dato").asText()).isEqualTo("2026-01-06")
        assertThat(vurdering.at("/vurderingerMeta/kvalitetssikretAv")).isEmpty()
        assertThat(vurdering.at("/vurderingerMeta/besluttetAv")).isEmpty()
        assertThat(respons.path("bekreftelse").isNull).isTrue()
    }

    @Test
    fun `returnerer ikke bekreftelse så lenge brevbekreftelsen er åpen`() {
        val behandling = opprettBehandlingMedSøknad()
        lagreVurdering(behandling)
        avklaringsbehovene(behandling).opprettBekreftelse()

        val respons = hentGrunnlag(behandling)

        assertThat(respons.path("bekreftelse").isNull).isTrue()
    }

    @Test
    fun `returnerer bekreftelse med vurderingerMeta fra avklaringsbehovet når brevet er bekreftet`() {
        val behandling = opprettBehandlingMedSøknad()
        lagreVurdering(behandling)
        val avklaringsbehovene = avklaringsbehovene(behandling)
        avklaringsbehovene.opprettBekreftelse()
        avklaringsbehovene.løsAvklaringsbehov(
            Definisjon.BEKREFT_BREV_SENDT_MANUELT, "Brev sendt", saksbehandler, kreverToTrinn = false
        )

        val respons = hentGrunnlag(behandling)

        assertThat(respons.at("/bekreftelse/vurderingerMeta/vurdertAv/ident").asText())
            .isEqualTo(saksbehandler.ident)
        assertThat(respons.at("/bekreftelse/vurderingerMeta/vurdertAv/dato").asText())
            .isEqualTo(LocalDate.now().toString())
    }

    private fun avklaringsbehovene(behandling: Behandling) =
        Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)

    private fun Avklaringsbehovene.opprettBekreftelse() = opprett(
        definisjon = Definisjon.BEKREFT_BREV_SENDT_MANUELT,
        funnetISteg = Definisjon.BEKREFT_BREV_SENDT_MANUELT.løsesISteg,
        perioderSomIkkeErTilstrekkeligVurdert = null,
        perioderVedtaketBehøverVurdering = null,
        perioderKanVurderes = null,
        gradBehov = null,
    )

    private fun lagreVurdering(behandling: Behandling) {
        InMemorySøknadUtenlandsoppholdRepository.lagre(
            behandling.id,
            SøknadUtenlandsoppholdVurdering(
                søknadsperiode = Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14)),
                land = "SE",
                resultat = SøknadUtenlandsoppholdResultat.INNVILGET,
                godkjentPeriode = Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 14)),
                antallGodkjenteDager = null,
                begrunnelse = "Ferie i Sverige",
                vurdertAv = saksbehandler,
                vurdertTidspunkt = LocalDateTime.of(2026, 1, 6, 10, 0),
            )
        )
    }

    private fun opprettBehandlingMedSøknad(): Behandling {
        val sak = opprettInMemorySak()
        val behandling = opprettBehandling(sak, TypeBehandling.Utenlandsopphold)
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, "123"),
                sakId = sak.id,
                behandlingId = behandling.id,
                mottattTidspunkt = mottattDato.atStartOfDay(),
                type = InnsendingType.SØKNAD_UTENLANDSOPPHOLD,
                kanal = Kanal.PAPIR,
                strukturertDokument = StrukturertDokument(SøknadUtenlandsoppholdV0(mottattDato = mottattDato)),
            )
        )
        return behandling
    }

    private fun hentGrunnlag(behandling: Behandling): JsonNode {
        lateinit var respons: JsonNode

        testApplication {
            installApplication {
                søknadUtenlandsoppholdGrunnlagApi(
                    dataSource = MockDataSource(),
                    repositoryRegistry = inMemoryRepositoryRegistry,
                    gatewayProvider = testGatewayProvider(),
                )
            }

            val response = createClient()
                .get("/api/behandling/${behandling.referanse.referanse}/grunnlag/soknad-utenlandsopphold") {
                    bearerAuth(AzureTokenGen("behandlingsflyt").generate(false, "behandlingsflyt", "Z123456"))
                }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            respons = response.body()
        }

        return respons
    }
}
