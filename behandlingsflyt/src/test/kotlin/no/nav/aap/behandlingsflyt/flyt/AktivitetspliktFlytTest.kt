package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackMeldekort
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class AktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackMeldekort::class as KClass<UnleashGateway>) {

    @Test
    fun `Happy-case flyt for aktivitetsplikt 11_7`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        dataSource.transaction { connection ->
            assertThat(
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(åpenBehandling.id)
            ).isNull()
        }

        // TODO: Mekanisme for opprettelse og automatisk prosessering
        var aktivitetspliktBehandling = opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Aktivitetsplikt,
            vurderingsbehov = listOf(
                VurderingsbehovMedPeriode(
                    Vurderingsbehov.AKTIVITETSPLIKT_11_7,
                    periode = sak.rettighetsperiode
                )
            ),
            forrigeBehandlingId = null
        )

        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.OPPRETTET)

        prosesserBehandling(aktivitetspliktBehandling)

        aktivitetspliktBehandling = hentBehandling(aktivitetspliktBehandling.referanse)
            .medKontekst {
                assertThat(this.behandling).extracting { it.aktivtSteg() }
                    .isEqualTo(StegType.VURDER_AKTIVITETSPLIKT_11_7)
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.VURDER_BRUDD_11_7)

            }.løsAvklaringsBehov(
                VurderBrudd11_7Løsning(
                    aktivitetsplikt11_7Vurdering = Aktivitetsplikt11_7LøsningDto(
                        begrunnelse = "Brudd",
                        erOppfylt = false,
                        utfall = Utfall.STANS,
                        gjelderFra = sak.rettighetsperiode.fom.plusWeeks(20)
                    )
                )
            ).medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)
            }.fattVedtakEllerSendRetur().medKontekst {
                assertThat(this.behandling).extracting { it.aktivtSteg() }
                    .isEqualTo(StegType.IVERKSETT_BRUDD)
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)
            }

        val grunnlagIAktivitetspliktBehandling = dataSource.transaction { connection ->
            Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(aktivitetspliktBehandling.id)

        }
        assertThat(grunnlagIAktivitetspliktBehandling).isNotNull


        // TODO: Mekanisme for opprettelse og automatisk prosessering
        val opprettetBehandling = dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            SakOgBehandlingService(repositoryProvider, gatewayProvider).finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT, vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            Vurderingsbehov.MOTTATT_AKTIVITETSMELDING,
                            periode = sak.rettighetsperiode
                        )
                    )
                )
            )
        }
        assertThat(opprettetBehandling is SakOgBehandlingService.MåBehandlesAtomært)
        val atomærBehandling = opprettetBehandling as SakOgBehandlingService.MåBehandlesAtomært
        var (grunnlagIFasttrackBehandling, grunnlagIÅpenBehandling) = dataSource.transaction { connection ->
            Pair(
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(atomærBehandling.nyBehandling.id),
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(atomærBehandling.åpenBehandling!!.id)
            )

        }
        assertThat(grunnlagIFasttrackBehandling).isNull()
        assertThat(grunnlagIÅpenBehandling).isNull()

        dataSource.transaction { connection ->
            ProsesserBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).triggProsesserBehandling(
                opprettetBehandling
            )
        }
        grunnlagIFasttrackBehandling = dataSource.transaction { connection ->
            Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(atomærBehandling.nyBehandling.id)

        }
        grunnlagIÅpenBehandling = dataSource.transaction { connection ->
            Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(opprettetBehandling.åpenBehandling!!.id)

        }
        assertThat(grunnlagIFasttrackBehandling).isNotNull
        assertThat(grunnlagIÅpenBehandling).isNotNull
    }
}