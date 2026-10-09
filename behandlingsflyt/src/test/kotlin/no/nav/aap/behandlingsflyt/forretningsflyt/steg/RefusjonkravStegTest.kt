package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.ARENA_MIGRERING_BRUKER
import no.nav.aap.behandlingsflyt.arena.ArenaMigreringService
import no.nav.aap.behandlingsflyt.arena.ArenaRefusjonskrav
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class RefusjonkravStegTest {
    private val behandlingRepository = InMemoryBehandlingRepository
    private val gatewayProvider = createGatewayProvider {
        register<AlleAvskruddUnleash>()
    }

    private val migreringUnleash = FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(BehandlingsflytFeature.MigrerRefusjonskravFraArenaAutomatisk)
    )

    private val revurderingUnleash = FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(BehandlingsflytFeature.KanVurdereRefusjonIRevurdering)
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
                    vurdertAv = ARENA_MIGRERING_BRUKER
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
    fun `migrering lagrer payload fra Arena for sporing`() {
        val sak = opprettInMemorySak(1 januar 2020)
        val behandling = opprettBehandling(sak)
        val arenaService = standardArenaMigreringService()

        nyttSteg(mockk(relaxed = true), migreringUnleash, arenaMigreringService = arenaService)
            .utfør(migreringsKontekst(sak, behandling))

        verify(exactly = 1) {
            arenaService.lagreMigreringsdataForSporing(
                behandling.id,
                StegType.REFUSJON_KRAV,
                null
            )
        }
    }

    @Test
    fun `migrering feiler men lagrer payload når Arena har refusjonskrav REFKRAVSOS`() {
        val sak = opprettInMemorySak(1 januar 2020)
        val behandling = opprettBehandling(sak)
        val arenaService = standardArenaMigreringService(ArenaRefusjonskrav("REFKRAVSOS", 1 januar 2020, null))
        val refusjonkravRepository: RefusjonkravRepository = mockk(relaxed = true)

        val steg = nyttSteg(refusjonkravRepository, migreringUnleash, arenaMigreringService = arenaService)

        assertThatThrownBy { steg.utfør(migreringsKontekst(sak, behandling)) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("REFKRAVSOS")

        verify(exactly = 1) { arenaService.lagreMigreringsdataForSporing(behandling.id, StegType.REFUSJON_KRAV, any()) }
        verify(exactly = 0) { refusjonkravRepository.lagre(any(), any(), any()) }
    }

    @Test
    fun `ikke-migrering oppfører seg som før og kan løfte avklaringsbehovet`() {
        val søknadsdato = 1 januar 2020
        val sak = opprettInMemorySak(søknadsdato)
        val behandling = opprettBehandling(
            sak = sak,
            vurderingsbehov = Vurderingsbehov.MOTTATT_SØKNAD,
            årsak = ÅrsakTilOpprettelse.SØKNAD
        )

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

    @Test
    fun `revurdering av migrert sak med automatisk vurdering løfter avklaringsbehov når refusjonskrav er vurderingsbehov`() {
        val sak = opprettInMemorySak(1 januar 2020)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val kontekst = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.REFUSJONSKRAV)
        }

        val steg = nyttSteg(
            refusjonkravRepositoryMedAutomatiskVurdering(),
            revurderingUnleash,
            TypeBehandling.Revurdering
        )

        steg.utfør(kontekst)

        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isTrue
    }

    @Test
    fun `revurdering av migrert sak med automatisk vurdering løfter ikke avklaringsbehov uten refusjonskrav som vurderingsbehov`() {
        val sak = opprettInMemorySak(1 januar 2020)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val kontekst = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
            this.vurderingsbehovRelevanteForSteg = emptySet()
        }

        val steg = nyttSteg(
            refusjonkravRepositoryMedAutomatiskVurdering(),
            revurderingUnleash,
            TypeBehandling.Revurdering
        )

        steg.utfør(kontekst)

        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isFalse
    }

    @Test
    fun `revurdering løfter ikke avklaringsbehov når KanVurdereRefusjonIRevurdering er avskrudd`() {
        val sak = opprettInMemorySak(1 januar 2020)
        val behandling = opprettBehandling(sak, TypeBehandling.Revurdering)
        val kontekst = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
            this.behandling = behandling
            this.rettighetsperiode = sak.rettighetsperiode
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.REFUSJONSKRAV)
        }

        val steg = nyttSteg(
            refusjonkravRepositoryMedAutomatiskVurdering(),
            AlleAvskruddUnleash,
            TypeBehandling.Revurdering
        )

        steg.utfør(kontekst)

        assertThat(hentRefusjonkravbehov(behandling)?.erÅpent() ?: false).isFalse
    }

    private fun refusjonkravRepositoryMedAutomatiskVurdering(): RefusjonkravRepository = mockk(relaxed = true) {
        every { hentHvisEksisterer(any()) } returns listOf(
            RefusjonkravVurdering(harKrav = false, navKontor = null, vurdertAv = ARENA_MIGRERING_BRUKER)
        )
    }

    private fun nyttSteg(
        refusjonkravRepository: RefusjonkravRepository,
        unleashGateway: no.nav.aap.behandlingsflyt.unleash.UnleashGateway,
        behandlingstype: TypeBehandling = TypeBehandling.Førstegangsbehandling,
        arenaMigreringService: ArenaMigreringService = standardArenaMigreringService(),
    ): RefusjonkravSteg {
        val behandlingService = mockk<BehandlingService>(relaxed = true) {
            every { utledFaktiskBehandlingstype(any<Behandling>()) } returns behandlingstype
        }
        val behandlingRepo = mockk<BehandlingRepository>(relaxed = true)

        return RefusjonkravSteg(
            refusjonkravRepository = refusjonkravRepository,
            tidligereVurderinger = FakeTidligereVurderinger(),
            avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, gatewayProvider),
            behandlingRepository = behandlingRepo,
            behandlingService = behandlingService,
            unleashGateway = unleashGateway,
            arenaMigreringService = arenaMigreringService
        )
    }

    private fun standardArenaMigreringService(refusjonskrav: ArenaRefusjonskrav? = null): ArenaMigreringService =
        mockk(relaxed = true) {
            every { hentRefusjonskrav(any()) } returns refusjonskrav
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

    private fun opprettBehandling(
        sak: Sak,
        typeBehandling: TypeBehandling = TypeBehandling.Førstegangsbehandling,
        vurderingsbehov: Vurderingsbehov = Vurderingsbehov.MIGRERING_FRA_ARENA,
        årsak: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.MIGRERING_FRA_ARENA
    ): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(vurderingsbehov)),
                årsak = årsak
            )
        )
}
