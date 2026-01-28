package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType.VURDER_RETTIGHETSPERIODE
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@Tag("motor")
@ParameterizedClass
@MethodSource("unleashTestDataSource")
class FlytOrkestratorTest(unleashGateway: KClass<UnleashGateway>) : AbstraktFlytOrkestratorTest(unleashGateway) {
    @Test
    fun `hopper over foreslå vedtak-steg når revurdering ikke skal innom NAY`() {
        val sak = happyCaseFørstegangsbehandling()
        // Revurdering av sykdom uten 11-13
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = true
        )
            .løsBistand(sak.rettighetsperiode.fom)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal gå rett til beslutter når ingen avklaringsbehov trenger å løses av NAY"
                }.containsExactly(Definisjon.FATTE_VEDTAK)
            }
    }

    @Test
    fun `revurdering skal innom foreslå vedtak-steg når NAY-saksbehandler har løst avklaringsbehov`() {
        val sak = happyCaseFørstegangsbehandling()
        // Revurdering som krever 11-13-vurdering
        revurdereFramTilOgMedSykdom(
            sak = sak,
            gjelderFra = sak.rettighetsperiode.fom,
            vissVarighet = false
        )
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "test",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = sak.rettighetsperiode.fom
                    ),
                    behovstype = Definisjon.AVKLAR_SYKEPENGEERSTATNING.kode
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).describedAs {
                    "Revurdering av sykdom skal innom foreslå vedtak-steg når vurdering av sykepengeerstatning er gjort av NAY"
                }.containsExactly(Definisjon.FORESLÅ_VEDTAK)
            }
    }

    @Test
    fun `kan tilbakeføre behandling til start`() {
        // Given:
        val (_, behandling) = sendInnFørsteSøknad()

        behandling.medKontekst {
            assertThat(åpneAvklaringsbehov)
                .extracting<Definisjon> { it.definisjon }
                .containsOnly(Definisjon.AVKLAR_SYKDOM)
        }

        val antallKjøringerVurderRettighetsperiode = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }

        // When:
        dataSource.transaction { connection ->
            val driftfunksjoner = Driftfunksjoner(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            driftfunksjoner.kjørFraSteg(behandling, VURDER_RETTIGHETSPERIODE)
        }

        // Then:
        // Har kjørt steget vi rullet tilbake til én gang til
        val antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring = dataSource.transaction { connection ->
            BehandlingRepositoryImpl(connection).hentStegHistorikk(behandling.id)
                .count { it.steg() == VURDER_RETTIGHETSPERIODE && it.status() == StegStatus.AVSLUTTER }
        }
        assertThat(antallKjøringerVurderRettighetsperiodeEtterTilbakekjøring)
            .isEqualTo(antallKjøringerVurderRettighetsperiode + 1)

        // Tilbake til AVKLAR_SYKDOM
        dataSource.transaction { connection ->
            assertThat(BehandlingRepositoryImpl(connection).hentAktivtSteg(behandling.id))
                .extracting { it?.steg() }
                .isEqualTo(StegType.AVKLAR_SYKDOM)
        }
    }
}
