package no.nav.aap.behandlingsflyt.integrasjon.brev

import no.nav.aap.behandlingsflyt.behandling.brev.AvslagBrev
import no.nav.aap.behandlingsflyt.behandling.brev.Innvilgelse
import no.nav.aap.behandlingsflyt.behandling.brev.SykdomsvurderingForBrevFormatter
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

@Fakes
class BrevGatewayTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `sender begrunnelse og eventuelle kilder gjennom eksisterende brevkontrakt`(brukApiV3: Boolean) {
        val dato = LocalDate.of(2026, 1, 20)
        val kilde = Dokumentkilde("123", "456", "Legeerklæring", dato)
        listOf(emptyList(), listOf(kilde)).forEach { kilder ->
            val grunnlag = SykdomsvurderingForBrev(
                BehandlingId(1), "Individuell begrunnelse", Bruker("Veileder"), kilder = kilder
            )
            val tekst = SykdomsvurderingForBrevFormatter.formater(grunnlag)
            val forventet = if (kilder.isEmpty()) {
                "Individuell begrunnelse"
            } else {
                "Individuell begrunnelse\n\nVi har lagt vekt på følgende dokumenter i vurderingen:\n- Legeerklæring (20.01.2026)"
            }
            val brevbehov = listOf(
                Innvilgelse(
                    virkningstidspunkt = dato,
                    sisteDagMedYtelse = dato.plusYears(1),
                    grunnlagBeregning = null,
                    tilkjentYtelse = null,
                    sykdomsvurdering = tekst,
                    forholdTilAndreYtelser = null,
                ),
                AvslagBrev.Avslag(sykdomsvurdering = tekst),
                AvslagBrev.AvslagSykdomsvilkåret(sykdomsvurdering = tekst)
            )
            brevbehov.forEach { behov ->
                val unikReferanse = UUID.randomUUID().toString()
                BrevGateway().bestillBrev(
                    saksnummer = Saksnummer("123"),
                    brukerIdent = Ident("12345678901"),
                    behandlingReferanse = BehandlingReferanse(UUID.randomUUID()),
                    unikReferanse = unikReferanse,
                    brevBehov = behov,
                    vedlegg = null,
                    ferdigstillAutomatisk = false,
                    signaturer = emptyList(),
                    brukApiV3 = brukApiV3,
                )

                val request = requireNotNull(FakeServers.hentBrevbestilling(unikReferanse))
                assertThat(request.faktagrunnlag.filterIsInstance<Faktagrunnlag.Sykdomsvurdering>())
                    .containsExactly(Faktagrunnlag.Sykdomsvurdering(forventet))
            }
        }
    }
}
