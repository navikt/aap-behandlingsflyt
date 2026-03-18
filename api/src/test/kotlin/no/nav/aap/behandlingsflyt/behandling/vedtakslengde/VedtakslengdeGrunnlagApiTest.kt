package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.aap.behandlingsflyt.BaseApiTest
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomInfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakslengdeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@Fakes
class VedtakslengdeGrunnlagApiTest : BaseApiTest() {

    @Test
    fun `henter vedtakslengde grunnlag uten vurderinger`() {
        val sak = nySak(søknadsDato = LocalDate.of(2024, 1, 1))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        testApplication {
            installApplication {
                vedtakslengdeGrunnlagApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                })
            }

            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/vedtakslengde") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val body = response.body<VedtakslengdeGrunnlagResponse>()
            assertThat(body.sisteVedtatteVurderinger).isEmpty()
            assertThat(body.nyeVurderinger).isEmpty()
        }
    }

    @Test
    fun `henter vedtakslengde grunnlag med nye vurderinger`() {
        val sak = nySak(søknadsDato = LocalDate.of(2024, 1, 1))
        val behandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)
        val sluttdato = LocalDate.of(2025, 1, 1)

        InMemoryVedtakslengdeRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = listOf(
                VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.parse("2024-01-01T12:00:00Z"),
                    begrunnelse = "Automatisk vurdert",
                )
            )
        )

        testApplication {
            installApplication {
                vedtakslengdeGrunnlagApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                })
            }

            val response = createClient().get("/api/behandling/${behandling.referanse.referanse}/grunnlag/vedtakslengde") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val body = response.body<VedtakslengdeGrunnlagResponse>()
            assertThat(body.sisteVedtatteVurderinger).isEmpty()
            assertThat(body.nyeVurderinger).hasSize(1)
            assertThat(body.nyeVurderinger.first().sluttdato).isEqualTo(sluttdato)
            assertThat(body.nyeVurderinger.first().fom).isEqualTo(sak.rettighetsperiode.fom)
        }
    }

    @Test
    fun `henter vedtakslengde grunnlag med vedtatte vurderinger og nye vurderinger`() {
        val søknadsDato = LocalDate.of(2024, 1, 1)
        val sak = nySak(søknadsDato = søknadsDato)
        val førstegangsbehandling = opprettBehandling(sak, TypeBehandling.Førstegangsbehandling)

        val sluttdatoFørstegangsbehandling = LocalDate.of(2025, 1, 1)
        val sluttdatoFørstegangsbehandlingManuell = LocalDate.of(2025, 5, 1)

        InMemoryVedtakslengdeRepository.lagre(
            behandlingId = førstegangsbehandling.id,
            vurderinger = listOf(
                VedtakslengdeVurdering(
                    sluttdato = sluttdatoFørstegangsbehandling,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = førstegangsbehandling.id,
                    opprettet = Instant.parse("2024-01-01T12:00:00Z"),
                    begrunnelse = "Automatisk vurdert",
                ),
                VedtakslengdeVurdering(
                    sluttdato = sluttdatoFørstegangsbehandlingManuell,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = Bruker("Saksbehandler"),
                    vurdertIBehandling = førstegangsbehandling.id,
                    opprettet = Instant.parse("2024-01-01T12:30:00Z"),
                    begrunnelse = "Den automatiske vurderingen var ikke god nok",
                )
            )
        )

        // Opprett revurdering
        val revurdering = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

        val sluttdatoRevurdering = LocalDate.of(2026, 1, 1)
        val sluttdatoRevurderingManuell = LocalDate.of(2026, 5, 1)

        InMemoryVedtakslengdeRepository.lagre(
            behandlingId = revurdering.id,
            vurderinger = listOf(
                VedtakslengdeVurdering(
                    sluttdato = sluttdatoRevurdering,
                    utvidetMed = ÅrMedHverdager.ANDRE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = revurdering.id,
                    opprettet = Instant.parse("2025-01-01T12:00:00Z"),
                    begrunnelse = "Automatisk vurdert",
                ),
                VedtakslengdeVurdering(
                    sluttdato = sluttdatoRevurderingManuell,
                    utvidetMed = ÅrMedHverdager.ANDRE_ÅR,
                    vurdertAv = Bruker("Saksbehandler"),
                    vurdertIBehandling = revurdering.id,
                    opprettet = Instant.parse("2025-01-01T12:30:00Z"),
                    begrunnelse = "Vurdert manuelt",
                )
            )
        )

        testApplication {
            installApplication {
                vedtakslengdeGrunnlagApi(MockDataSource(), inMemoryRepositoryRegistry, createGatewayProvider {
                    register<NomInfoGateway>()
                    register<NorgGateway>()
                })
            }

            val response = createClient().get("/api/behandling/${revurdering.referanse.referanse}/grunnlag/vedtakslengde") {
                header("Authorization", "Bearer ${getToken().token()}")
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)

            val body = response.body<VedtakslengdeGrunnlagResponse>()

            // Vedtatte vurderinger blir redusert ned til kun en "gjeldende" vurdering (den manuelle overstyrer automatisk)
            assertThat(body.sisteVedtatteVurderinger).hasSize(1)
            assertThat(body.sisteVedtatteVurderinger[0].fom).isEqualTo(søknadsDato)
            assertThat(body.sisteVedtatteVurderinger[0].tom).isEqualTo(sluttdatoFørstegangsbehandlingManuell)

            // Nye vurderinger fra revurderingen
            assertThat(body.nyeVurderinger).hasSize(2)
            assertThat(body.nyeVurderinger[0].sluttdato).isEqualTo(sluttdatoRevurdering)
            assertThat(body.nyeVurderinger[0].fom).isEqualTo(sluttdatoFørstegangsbehandlingManuell.plusDays(1))
            assertThat(body.nyeVurderinger[1].sluttdato).isEqualTo(sluttdatoRevurderingManuell)
            assertThat(body.nyeVurderinger[1].fom).isEqualTo(sluttdatoFørstegangsbehandlingManuell.plusDays(1))
        }
    }
}



