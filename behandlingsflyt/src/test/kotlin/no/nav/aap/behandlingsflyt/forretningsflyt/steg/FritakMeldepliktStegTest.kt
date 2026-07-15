package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.FritakFraMeldepliktLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertFritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.PeriodisertFritaksvurderingDto
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMeldepliktRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FritakMeldepliktStegTest {

    @Test
    fun `legge til vurderingsbehov gir avklaringsbehov og det løses ved innsending`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val steg = konstruerSteg()

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.VURDER_FRITAK_MELDEPLIKT)
        }

        steg.utfør(kontekst)

        val behov = hentAvklaringsbehovet(behandling)

        assertThat(behov).isNotNull
        assertThat(behov!!.erÅpent()).isTrue

        sendInnLøsning(behandling, sak)

        steg.utfør(kontekst)
        val behov2 = hentAvklaringsbehovet(behandling)

        assertThat(behov2).isNotNull
        assertThat(behov2!!.erÅpent()).isFalse
    }

    @Test
    fun `om løsningen er nyere enn vurderingsbehovet, lukk avklaringsbehov`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val steg = konstruerSteg()

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingsbehovRelevanteForStegMedPerioder =
                setOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.VURDER_FRITAK_MELDEPLIKT,
                        oppdatertTid = LocalDateTime.now().minusDays(1)
                    )
                )
        }

        val behovFørLøsning = hentAvklaringsbehovet(behandling)
        assertThat(behovFørLøsning?.erÅpent()).isNull()

        sendInnLøsning(behandling, sak)
        steg.utfør(kontekst)


        steg.utfør(kontekst)

        val behov = hentAvklaringsbehovet(behandling)

        assertThat(behov).isNotNull
        assertThat(behov!!.erÅpent()).isFalse
    }

    private fun sendInnLøsning(
        behandling: Behandling,
        sak: Sak
    ) {
        FritakFraMeldepliktLøser(inMemoryRepositoryProvider).løs(
            AvklaringsbehovKontekst(
                Bruker("SAKSBEHANDLER"),
                behandling.flytKontekst()
            ), PeriodisertFritakMeldepliktLøsning(
                løsningerForPerioder = listOf(
                    PeriodisertFritaksvurderingDto(
                        begrunnelse = "...",
                        fom = sak.rettighetsperiode.fom,
                        tom = null,
                        harFritak = true
                    )
                )
            )
        )

        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        avklaringsbehovene.leggTilFrivilligHvisMangler(Definisjon.FRITAK_MELDEPLIKT, bruker = Bruker("SAKSBEHANDLER"))
        avklaringsbehovene.løsAvklaringsbehov(
            Definisjon.FRITAK_MELDEPLIKT,
            begrunnelse = "...",
            endretAv = "...",
            kreverToTrinn = false,
        )
    }

    private fun hentAvklaringsbehovet(behandling: Behandling): Avklaringsbehov? = Avklaringsbehovene(
        InMemoryAvklaringsbehovRepository,
        behandling.id
    ).hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)

    private fun konstruerSteg(): FritakMeldepliktSteg = FritakMeldepliktSteg(
        AvklaringsbehovService(inMemoryRepositoryProvider, minimalGatewayProvider()),
        avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
        tidligereVurderinger = FakeTidligereVurderinger(),
        meldepliktRepository = InMemoryMeldepliktRepository
    )
}