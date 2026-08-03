package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AvklarStønadsperiodeStegTest {

    private lateinit var steg: AvklarStønadsperiodeSteg

    @BeforeEach
    fun setup() {
        steg = AvklarStønadsperiodeSteg(
            unleashGateway = KravStegPåskruddUnleash,
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
        )
    }

    @Test
    fun `ingen relevante krav - lagrer tomt grunnlag`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val grunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)
        assertThat(grunnlag).isNotNull()
        assertThat(grunnlag!!.vurderinger).isEmpty()
    }

    @Test
    fun `førstegangsbehandling med ett relevant krav - lagrer én vurdering med riktig referanse`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 1, 15))
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.single().referanse).isEqualTo(krav.referanse)
        assertThat(vurderinger.single().relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
    }

    @Test
    fun `revurdering - arver vedtatte stønadsperioder fra forrige behandling og lager kun nye`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)

        val kravFraForrige = lagRelevantKrav(forrigeBehandling.id, LocalDate.of(2023, 6, 1))
        val stønadsperiodeFraForrige = lagStønadsperiodeVurdering(kravFraForrige.referanse, forrigeBehandling.id)
        InMemoryStønadsperiodeRepository.lagre(forrigeBehandling.id, setOf(stønadsperiodeFraForrige))

        val nyttKrav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 6, 1))
        InMemoryKravRepository.lagre(behandling.id, setOf(kravFraForrige, nyttKrav))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(2)
        assertThat(vurderinger.map { it.referanse })
            .containsExactlyInAnyOrder(kravFraForrige.referanse, nyttKrav.referanse)
        assertThat(vurderinger.single { it.referanse == kravFraForrige.referanse }.vurdertIBehandling)
            .isEqualTo(forrigeBehandling.id)
        assertThat(vurderinger.single { it.referanse == nyttKrav.referanse }.vurdertIBehandling)
            .isEqualTo(behandling.id)
    }

    @Test
    fun `re-kjøring av steget gir stabilt grunnlag uten duplikater`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 1, 1))
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.single().referanse).isEqualTo(krav.referanse)
    }
    
    private fun opprettFørstegangsbehandling(sakId: SakId) =
        InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

    private fun opprettRevurdering(sakId: SakId, forrigeBehandlingId: BehandlingId) =
        InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandlingId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

    private fun lagRelevantKrav(
        behandlingId: BehandlingId,
        mottattDato: LocalDate,
    ) = RelevantKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = JournalpostId(mottattDato.toString()),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(mottattDato, SøknadsdatoÅrsak.SøknadMottatt),
        overstyrMuligRettFra = null,
        muligRettFra = mottattDato,
    )

    private fun lagStønadsperiodeVurdering(
        referanse: Kravreferanse,
        behandlingId: BehandlingId,
        startDato: LocalDate = LocalDate.now(),
    ) = StønadsperiodeVurdering(
        referanse = referanse,
        opprettet = Instant.now(),
        vurdertIBehandling = behandlingId,
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        harHattOrdinærSiste52Uker = false,
        harGjenværendeKvote = false,
        relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
        startDato = startDato,
    )

    private object KravStegPåskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.KravSteg)
    )
}
