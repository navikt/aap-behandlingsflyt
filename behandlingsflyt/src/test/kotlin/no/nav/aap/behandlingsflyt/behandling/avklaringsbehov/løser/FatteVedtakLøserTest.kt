package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.MockDataSource
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class FatteVedtakLøserTest {
    private val repositoryRegistry = RepositoryRegistry()
        .register(InMemorySakRepository::class)
        .register(InMemoryAvklaringsbehovRepository::class)
        .register(InMemoryBehandlingRepository::class)

    @Test
    fun `Skal ikke reåpne behov før det som det returneres til`() {

        val (sak, behandling) = opprettPersonBehandlingOgSak()
        val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

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

        val fatteVedtakLøser = MockDataSource().transaction {
            FatteVedtakLøser(repositoryRegistry.provider(it), createGatewayProvider {register<FakeUnleash>() })
        }

        // Totrinnsvurdering ikke godkjent.
        fatteVedtakLøser.løs(
            AvklaringsbehovKontekst(
                bruker = Bruker("123"),
                kontekst = FlytKontekst(
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    forrigeBehandlingId = behandling.forrigeBehandlingId,
                    behandlingType = TypeBehandling.Førstegangsbehandling,
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
            Person(
                PersonId(Random().nextLong()),
                UUID.randomUUID(),
                listOf(genererIdent(LocalDate.now().minusYears(23)))
            )
        val sak = InMemorySakRepository.finnEllerOpprett(
            person,
            periode = Periode(LocalDate.now(), LocalDate.now().plusDays(5)),
        )
        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            vurderingsbehov = listOf(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null
        )
        return Pair(sak, behandling)
    }
}