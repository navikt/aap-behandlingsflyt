package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class HarTilgangOgKanSaksbehandleTest {
    @BeforeEach
    fun setup() {
        InMemoryAvklaringsbehovRepository.clearMemory()
    }

    @Test
    fun `Når det finnes åpne avklaringsbehov som trenger kvalitetssikring`() {
        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        val harTilgangTilÅSaksbehandle =
            harTilgangOgKanSaksbehandle(harTilgang = true, avklaringsbehovene = avklaringsbehovene)

        assertThat(harTilgangTilÅSaksbehandle).isTrue()
    }

    @Test
    fun `Når avklaringsbehovet for kvalitetssikring er løftet og under behandling`() {
        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.KVALITETSSIKRING,
            funnetISteg = StegType.KVALITETSSIKRING,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        val harTilgangTilÅSaksbehandle =
            harTilgangOgKanSaksbehandle(harTilgang = true, avklaringsbehovene = avklaringsbehovene)

        assertThat(harTilgangTilÅSaksbehandle).isFalse()
    }

    @Test
    fun `Når avklaringsbehovet for kvalitetssikring er avsluttet`() {
        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.KVALITETSSIKRING,
            funnetISteg = StegType.KVALITETSSIKRING,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        avklaringsbehovene.løsAvklaringsbehov(
            definisjon = Definisjon.KVALITETSSIKRING,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )
        val harTilgangTilÅSaksbehandle =
            harTilgangOgKanSaksbehandle(harTilgang = true, avklaringsbehovene = avklaringsbehovene)

        assertThat(harTilgangTilÅSaksbehandle).isFalse()
    }

    @Test
    fun `Når kvalitetssikrer har returnert`() {
        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.KVALITETSSIKRING,
            funnetISteg = StegType.KVALITETSSIKRING,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        avklaringsbehovene.vurderKvalitet(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            godkjent = false,
            begrunnelse = "Ikke godkjent",
            vurdertAv = "Kvalitetssikrer",
        )

        val harTilgangTilÅSaksbehandle =
            harTilgangOgKanSaksbehandle(harTilgang = true, avklaringsbehovene = avklaringsbehovene)

        assertThat(harTilgangTilÅSaksbehandle).isTrue()
    }

    @Test
    fun `Når beslutter har returnert`() {
        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.KVALITETSSIKRING,
            funnetISteg = StegType.KVALITETSSIKRING,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        avklaringsbehovene.løsAvklaringsbehov(
            definisjon = Definisjon.KVALITETSSIKRING,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )
        avklaringsbehovene.vurderTotrinn(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            godkjent = false,
            begrunnelse = "Ikke godkjent",
            vurdertAv = "Beslutter",
            årsakTilRetur = emptyList()
        )

        val harTilgangTilÅSaksbehandle =
            harTilgangOgKanSaksbehandle(harTilgang = true, avklaringsbehovene = avklaringsbehovene)

        assertThat(harTilgangTilÅSaksbehandle).isTrue()
    }
}