package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FakeAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySakRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class FatteVedtakLøserTest {
    @Test
    fun `Skal ikke reåpne behov før det som det returneres til`() {
        val (sak, behandling) = opprettPersonBehandlingOgSak()
        val avklaringsbehovRepository = FakeAvklaringsbehovRepository()

        // Oppretter avklaringsbehov på soning
        avklaringsbehovRepository.opprett(
            behandling.id, definisjon = Definisjon.AVKLAR_SONINGSFORRHOLD,
            funnetISteg = StegType.DU_ER_ET_ANNET_STED,
            frist = null,
            begrunnelse = "ddd",
            grunn = null,
            endretAv = "Fredrik"
        )

        // Oppretter og løser et avklaringsbehov på sykdom
        avklaringsbehovRepository.opprett(
            behandling.id, definisjon = Definisjon.AVKLAR_SYKDOM,
            funnetISteg = StegType.AVKLAR_SYKDOM,
            frist = null,
            begrunnelse = "ddd",
            grunn = null,
            endretAv = "Fredrik"
        )
        avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).løsAvklaringsbehov(
            definisjon = Definisjon.AVKLAR_SYKDOM,
            begrunnelse = "...",
            endretAv = "xxx",
        )

        val fatteVedtakLøser = FatteVedtakLøser(
            avklaringsbehovRepository = avklaringsbehovRepository,
            behandlingRepository = InMemoryBehandlingRepository
        )

        // Totrinnsvurdering ikke godkjent.
        fatteVedtakLøser.løs(
            AvklaringsbehovKontekst(
                bruker = Bruker("123"),
                kontekst = FlytKontekst(
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    behandlingType = TypeBehandling.Førstegangsbehandling
                )
            ),
            løsning = FatteVedtakLøsning(
                vurderinger = listOf(
                    TotrinnsVurdering(
                        definisjon = AvklaringsbehovKode.`5010`,
                        godkjent = false,
                        begrunnelse = "nei",
                        grunner = null
                    )
                ),
                behovstype = AvklaringsbehovKode.`5010`
            )
        )

        // Kun avklar soningsbehov er gjenåpnet
        assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).alle()).hasSize(2)
        assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).åpne()).hasSize(1)
        assertThat(avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id).åpne().first().definisjon).isEqualTo(
            Definisjon.AVKLAR_SONINGSFORRHOLD
        )
    }

    private fun opprettPersonBehandlingOgSak(): Pair<Sak, Behandling> {
        val person =
            Person(Random().nextLong(), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(
            person,
            periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
        )
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            årsaker = listOf(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            orginalBehandling = null
        )
        return Pair(sak, behandling)
    }
}