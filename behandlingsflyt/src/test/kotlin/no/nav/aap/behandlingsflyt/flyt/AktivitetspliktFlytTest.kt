package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate.BistandVurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7RepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackAktivitetsplikt
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import kotlin.reflect.KClass

class AktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackAktivitetsplikt::class as KClass<UnleashGateway>) {

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

        val aktivtStegFørEffektueringsbehandling = åpenBehandling.aktivtSteg()

        // TODO: Mekanisme for opprettelse og automatisk prosessering
        val opprettetEffektueringsbehandling = dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            SakOgBehandlingService(repositoryProvider, gatewayProvider).finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT, vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT,
                            periode = sak.rettighetsperiode
                        )
                    )
                )
            )
        }
        assertThat(opprettetEffektueringsbehandling is SakOgBehandlingService.MåBehandlesAtomært)
        val atomærBehandling = opprettetEffektueringsbehandling as SakOgBehandlingService.MåBehandlesAtomært
        assertThat(atomærBehandling.nyBehandling.typeBehandling() == TypeBehandling.Revurdering)
        assertThat(atomærBehandling.åpenBehandling!!.id).isEqualTo(åpenBehandling.id)
        
        var (grunnlagIEffektueringsbehandling, grunnlagIÅpenBehandling) = dataSource.transaction { connection ->
            Pair(
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(atomærBehandling.nyBehandling.id),
                Aktivitetsplikt11_7RepositoryImpl(connection)
                    .hentHvisEksisterer(atomærBehandling.åpenBehandling.id)
            )

        }
        assertThat(grunnlagIEffektueringsbehandling).isNull()
        assertThat(grunnlagIÅpenBehandling).isNull()

        dataSource.transaction { connection ->
            ProsesserBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).triggProsesserBehandling(
                opprettetEffektueringsbehandling
            )
        }
        motor.kjørJobber()
        
        val effektueringsbehandling = hentBehandling(atomærBehandling.nyBehandling.referanse)
        assertThat(effektueringsbehandling.status()).isEqualTo(Status.AVSLUTTET)
        
        grunnlagIEffektueringsbehandling = dataSource.transaction { connection ->
            Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(atomærBehandling.nyBehandling.id)

        }
        grunnlagIÅpenBehandling = dataSource.transaction { connection ->
            Aktivitetsplikt11_7RepositoryImpl(connection)
                .hentHvisEksisterer(opprettetEffektueringsbehandling.åpenBehandling!!.id)

        }
        assertThat(grunnlagIEffektueringsbehandling).isNotNull
        assertThat(grunnlagIÅpenBehandling).isEqualTo(grunnlagIEffektueringsbehandling)

        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs("Effektuering av aktivitetsplikt skal ikke endre steg for åpen behandling, dersom den åpne behandlingen står i steg før informasjonskravet")
            .isEqualTo(aktivtStegFørEffektueringsbehandling)
    }

    @Test
    fun `Åpen behandling skal trekkes tilbake ved effktuering av aktivitetsplikt`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        var åpenBehandling = revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom)

        åpenBehandling = åpenBehandling.løsAvklaringsBehov(
            AvklarBistandsbehovLøsning(
                bistandsVurdering = BistandVurderingLøsningDto(
                    begrunnelse = "Trenger hjelp fra nav",
                    erBehovForAktivBehandling = true,
                    erBehovForArbeidsrettetTiltak = false,
                    erBehovForAnnenOppfølging = null,
                    skalVurdereAapIOvergangTilUføre = null,
                    skalVurdereAapIOvergangTilArbeid = null,
                    overgangBegrunnelse = null
                ),
            )
        )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsAvklaringsBehov(
                SykdomsvurderingForBrevLøsning(
                    vurdering = "Begrunnelse"
                ),
            )
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = false,
                    ),
                ),
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }

        val aktivtStegIÅpenBehandlingFørEffektuering = åpenBehandling.aktivtSteg()
        assertThat(aktivtStegIÅpenBehandlingFørEffektuering).isEqualTo(StegType.FORESLÅ_VEDTAK)


        opprettAktivitetspliktBehandlingMedVurdering(
            sak,
            Status.AVSLUTTET,
            Aktivitetsplikt11_7Vurdering(
                begrunnelse = "Brudd",
                erOppfylt = false,
                utfall = Utfall.STANS,
                gjelderFra = sak.rettighetsperiode.fom.plusWeeks(18),
                vurdertAv = "Saksbehandler",
                opprettet = sak.rettighetsperiode.fom.plusWeeks(20).atStartOfDay().toInstant(ZoneOffset.UTC)
            )
        )

        val effektueringsbehandling = dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            SakOgBehandlingService(repositoryProvider, gatewayProvider).finnEllerOpprettBehandling(
                sak.id,
                VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT, vurderingsbehov = listOf(
                        VurderingsbehovMedPeriode(
                            Vurderingsbehov.EFFEKTUER_AKTIVITETSPLIKT,
                            periode = sak.rettighetsperiode
                        )
                    )
                )
            )
        }

        assertThat(effektueringsbehandling is SakOgBehandlingService.MåBehandlesAtomært)
        dataSource.transaction { connection ->
            ProsesserBehandlingService(
                postgresRepositoryRegistry.provider(connection),
                gatewayProvider
            ).triggProsesserBehandling(
                effektueringsbehandling
            )
        }

        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs{"Skal trekkes tilbake til steget informasjonskravet står på"}
            .isEqualTo(StegType.IKKE_OPPFYLT_MELDEPLIKT)
        
        motor.kjørJobber()
        åpenBehandling =
            dataSource.transaction { connection -> BehandlingRepositoryImpl(connection).hent(åpenBehandling.id) }
        assertThat(åpenBehandling.aktivtSteg())
            .describedAs { "Skal prosesseres automatisk og ende opp på samme steg som før" }
            .isEqualTo(aktivtStegIÅpenBehandlingFørEffektuering)
    }

    private fun opprettAktivitetspliktBehandlingMedVurdering(
        sak: Sak,
        status: Status,
        vurdering: Aktivitetsplikt11_7Vurdering,
        forrige: BehandlingId? = null,
    ): Behandling {
        return dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)

            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val Aktivitetsplikt11_7Repository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()

            val behandling = opprettAktivitetspliktBehandling(repositoryProvider, sak, forrige)

            Aktivitetsplikt11_7Repository.lagre(
                behandling.id, vurdering
            )
            behandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
            behandling
        }
    }

    private fun opprettAktivitetspliktBehandling(
        repositoryProvider: RepositoryProvider,
        sak: Sak,
        forrige: BehandlingId? = null
    ): Behandling {
        return repositoryProvider.provide<BehandlingRepository>().opprettBehandling(
            sak.id, TypeBehandling.Aktivitetsplikt,
            forrigeBehandlingId = forrige,
            VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.AKTIVITETSPLIKT_11_7)),
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
            )
        )
    }

}