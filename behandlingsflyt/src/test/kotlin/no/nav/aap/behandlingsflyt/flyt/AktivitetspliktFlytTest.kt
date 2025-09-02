package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7LøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeUnleashFasttrackAktivitetsplikt
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class AktivitetspliktFlytTest :
    AbstraktFlytOrkestratorTest(FakeUnleashFasttrackAktivitetsplikt::class as KClass<UnleashGateway>) {

    @Test
    fun `Happy-case flyt for aktivitetsplikt 11_7`() {
        val person = TestPersoner.STANDARD_PERSON()
        val sak = happyCaseFørstegangsbehandling(person = person)
        val førstegangsbehandling = hentNyesteBehandlingForSak(sak.id)

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
            forrigeBehandlingId = førstegangsbehandling.id
        )

        assertThat(aktivitetspliktBehandling.status()).isEqualTo(Status.OPPRETTET)

        prosesserBehandling(aktivitetspliktBehandling)

        hentBehandling(aktivitetspliktBehandling.referanse)
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
    }
}