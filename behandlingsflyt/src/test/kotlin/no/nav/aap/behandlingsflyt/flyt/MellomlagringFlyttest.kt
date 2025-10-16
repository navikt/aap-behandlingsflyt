package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.mellomlagring.MellomlagretVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.behandling.mellomlagring.MellomlagretVurderingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.KClass

@Tag("motor")
class MellomlagringFlyttest() : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `skal nullstille mellomlagret verdi når avklaringsbehov løses - og nullstille hengende mellomlagrede verdier ved iverksettelse`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val person = TestPersoner.STANDARD_PERSON()
        val ident = person.aktivIdent()

        val førstegangsbehandling = sendInnSøknad(ident, periode, TestSøknader.STANDARD_SØKNAD)
            .medKontekst {
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .mellomlagreSykdom()
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNotNull
            }
            .løsSykdom()
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNull()
            }
            .løsBistand()
            .mellomlagreSykdom()
            .løsRefusjonskrav()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy {
                    assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
                }
            }
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap()
            .løsOppholdskrav(periode.fom)
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNotNull()
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtakEllerSendRetur()
            .medKontekst {
                val mellomlagretVerdi = hentMellomlagretVerdi()
                assertThat(mellomlagretVerdi).isNull()
            }
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    private fun BehandlingInfo.hentMellomlagretVerdi(): MellomlagretVurdering? {
        val mellomlagretVerdi = dataSource.transaction { connection ->
            MellomlagretVurderingRepositoryImpl(connection)
                .hentHvisEksisterer(behandling.id, AvklaringsbehovKode.`5003`)
        }
        return mellomlagretVerdi
    }

}