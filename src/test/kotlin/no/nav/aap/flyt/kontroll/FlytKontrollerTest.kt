package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøsning
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Førstegangsbehandling
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.grunnlag.yrkesskade.YrkesskadeRegister
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.person.PersonTjeneste
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import no.nav.aap.mottak.DokumentMottattPersonHendelse
import no.nav.aap.mottak.HendelsesMottak
import no.nav.aap.mottak.LøsAvklaringsbehovBehandlingHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FlytKontrollerTest {

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123123")
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Simulerer et svar fra YS-løsning om at det finnes en yrkesskade
        YrkesskadeRegister.konstruer(ident = ident, periode = periode)

        // Sender inn en søknad
        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))

        val fagsak = FagsakTjeneste.finnEllerOpprett(PersonTjeneste.finnEllerOpprett(ident), periode)
        assert(fagsak.saksnummer.toString().isNotEmpty())

        val behandling = BehandlingTjeneste.finnSisteBehandlingFor(fagsak.id).orElseThrow()
        assert(behandling.type == Førstegangsbehandling)

        assert(behandling.avklaringsbehov().isNotEmpty())
        assert(behandling.status() == Status.UTREDES)


        HendelsesMottak.håndtere(behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
        assert(behandling.avklaringsbehov().filter { it.erÅpent() }.any { it.definisjon == Definisjon.FORESLÅ_VEDTAK })
        assert(behandling.status() == Status.UTREDES)


        HendelsesMottak.håndtere(behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = AvklarYrkesskadeLøsning("Begrunnelse", "meg")
            )
        )

        HendelsesMottak.håndtere(behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = ForeslåVedtakLøsning("Begrunnelse", "meg")
            )
        )

        // Saken står til To-trinnskontroll hos beslutter
        assert(behandling.avklaringsbehov().filter { it.erÅpent() }.any { it.definisjon == Definisjon.FATTE_VEDTAK })
        assert(behandling.status() == Status.UTREDES)

        HendelsesMottak.håndtere(behandling.id,
            LøsAvklaringsbehovBehandlingHendelse(
                versjon = 1L,
                løsning = FatteVedtakLøsning("Begrunnelse", "meg")
            )
        )

        assert(behandling.status() == Status.AVSLUTTET)
    }

    @Test
    fun `skal IKKE avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        val ident = Ident("123123123124")
        val person = PersonTjeneste.finnEllerOpprett(ident)
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        HendelsesMottak.håndtere(ident, DokumentMottattPersonHendelse(periode = periode))

        val fagsak = FagsakTjeneste.finnEllerOpprett(person, periode)
        assert(fagsak.saksnummer.toString().isNotEmpty())

        val behandling = BehandlingTjeneste.finnSisteBehandlingFor(fagsak.id).orElseThrow()
        assert(behandling.type == Førstegangsbehandling)

        assert(behandling.avklaringsbehov().isEmpty())
        assert(behandling.status() == Status.AVSLUTTET)
    }
}
