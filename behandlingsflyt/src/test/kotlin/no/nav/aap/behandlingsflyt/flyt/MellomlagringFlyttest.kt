package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Tag("motor")
class MellomlagringFlyttest : AbstraktFlytOrkestratorTest(AlleAvskruddUnleash::class) {

    @Test
    fun `skal nullstille mellomlagret verdi når avklaringsbehov løses - og nullstille hengende mellomlagrede verdier ved iverksettelse`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.STANDARD_PERSON()

        val førstegangsbehandling =
            sendInnFørsteSøknad(TestSøknader.STANDARD_SØKNAD, person, periode.fom.atStartOfDay()).second
                .medKontekst {
                    assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                }
            }
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy {
                    assertThat(it.definisjon)
                        .describedAs { "Er ikke tilstrekkelig vurdert dersom det finnes mellomlagret sykdomsvurdering" }
                        .isEqualTo(Definisjon.BEKREFT_VURDERINGER_OPPFØLGING)
                }
            }
            .slettMellomlagretVurdering(Definisjon.AVKLAR_SYKDOM)
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNull()
            }
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .mellomlagreSykdom()
            .løsOppholdskrav(periode.fom)
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNotNull()
            }
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNull()
            }
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    private fun BehandlingInfo.hentMellomlagretVerdi(): MellomlagretVurdering? {
        val mellomlagretVerdi =
            repositoryProvider.provide<MellomlagretVurderingRepository>()
                .hentHvisEksisterer(behandling.id, AvklaringsbehovKode.`5003`)
        return mellomlagretVerdi
    }
}
