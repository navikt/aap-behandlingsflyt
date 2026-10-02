package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RefusjonkravStegTest {
    private val behandlingRepository = InMemoryBehandlingRepository
    private val gatewayProvider = createGatewayProvider {
        register<AlleAvskruddUnleash>()
    }

    private val migreringUnleash = FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(BehandlingsflytFeature.MigrerRefusjonskravFraArenaAutomatisk)
    )

    @Test
    fun `migrering uten eksisterende grunnlag lagrer at det ikke er refusjonskrav og løfter ikke avklaringsbehov`() {
        val søknadsdato = 1 januar 2020
        val sak = opprettInMemorySak(søknadsdato)
        val behandling = opprettBehandling(sak)
        val kontekst = migreringsKontekst(sak, behandling)

        var lagret: List<RefusjonkravVurdering>? = null
        val refusjonkravRepository: RefusjonkravRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } answers { lagret }
            every { lagre(any(), any(), any()) } answers { lagret = thirdArg() }
        }

        val steg = nyttSteg(refusjonkravRepository, migreringUnleash)

        steg.utfør(kontekst)

        verify(exactly = 1) {
            refusjonkravRepository.lagre(
                sak.id,
                behandling.id,
                match { vurderinger: List<RefusjonkravVurdering> ->
                    vurderinger.size == 1 && !vurderinger[0].harKrav && vurderinger[0].fom == null && vurderinger[0].tom == null
                }
            )
        }
        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isFalse
    }

    @Test
    fun `migrering overskriver eksisterende grunnlag`() {
        val søknadsdato = 1 januar 2020
        val sak = opprettInMemorySak(søknadsdato)
        val behandling = opprettBehandling(sak)
        val kontekst = migreringsKontekst(sak, behandling)

        val refusjonkravRepository: RefusjonkravRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns listOf(
                RefusjonkravVurdering(
                    harKrav = false,
                    navKontor = null,
                    vurdertAv = no.nav.aap.behandlingsflyt.SYSTEMBRUKER
                )
            )
        }

        val steg = nyttSteg(refusjonkravRepository, migreringUnleash)

        steg.utfør(kontekst)

        verify(exactly = 1) {
            refusjonkravRepository.lagre(
                sak.id,
                behandling.id,
                match { vurderinger: List<RefusjonkravVurdering> ->
                    vurderinger.size == 1 && !vurderinger[0].harKrav
                }
            )
        }
        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isFalse
    }

    @Test
    fun `ikke-migrering oppfører seg som før og kan løfte avklaringsbehovet`() {
        val søknadsdato = 1 januar 2020
        val sak = opprettInMemorySak(søknadsdato)
        val behandling = opprettBehandling(sak)
        val kontekst = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
            this.vurderingType = VurderingType.FØRSTEGANGSBEHANDLING
        }

        val refusjonkravRepository: RefusjonkravRepository = mockk(relaxed = true) {
            every { hentHvisEksisterer(any()) } returns null
        }

        val steg = nyttSteg(refusjonkravRepository, AlleAvskruddUnleash)

        steg.utfør(kontekst)

        verify(exactly = 0) { refusjonkravRepository.lagre(any(), any(), any()) }
        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isTrue
    }

    private fun nyttSteg(
        refusjonkravRepository: RefusjonkravRepository,
        unleashGateway: no.nav.aap.behandlingsflyt.unleash.UnleashGateway
    ): RefusjonkravSteg {
        val behandlingService = mockk<BehandlingService>(relaxed = true) {
            every { utledFaktiskBehandlingstype(any<Behandling>()) } returns TypeBehandling.Førstegangsbehandling
        }
        val behandlingRepo = mockk<BehandlingRepository>(relaxed = true)

        return RefusjonkravSteg(
            refusjonkravRepository = refusjonkravRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
            avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, gatewayProvider),
            behandlingRepository = behandlingRepo,
            behandlingService = behandlingService,
            unleashGateway = unleashGateway
        )
    }

    private fun migreringsKontekst(sak: Sak, behandling: Behandling): FlytKontekstMedPerioder =
        no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
            this.vurderingType = VurderingType.MIGERING_FRA_ARENA
        }

    private fun hentRefusjonkravbehov(behandling: Behandling) =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.REFUSJON_KRAV)

    private fun opprettBehandling(sak: Sak): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
}
