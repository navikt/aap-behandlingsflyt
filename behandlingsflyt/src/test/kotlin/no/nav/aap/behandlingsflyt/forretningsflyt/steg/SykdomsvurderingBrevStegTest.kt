package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class SykdomsvurderingBrevStegTest {

    private val steg = SykdomsvurderingBrevSteg(
        InMemorySykdomsvurderingForBrevRepository,
        AvklaringsbehovService(inMemoryRepositoryProvider, createGatewayProvider { register<AlleAvskruddUnleash>() }),
        InMemoryAvklaringsbehovRepository,
        FakeTidligereVurderinger()
    )
    private val behandlingRepository = InMemoryBehandlingRepository

    @Test
    fun `skal opprette avklaringsbehov dersom ingen vurdering allerede finnes`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val kontekstMedPerioder = flytKontekstMedPerioder(behandling)

        opprettOgLøsSykdombehov(behandling)

        steg.utfør(kontekstMedPerioder)

        val behov = hentBrevBehov(behandling)

        assertThat(behov).isNotNull
    }

    @Test
    fun `skal returnere fullført dersom avklaringsbehov allerede finnes`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)

        InMemorySykdomsvurderingForBrevRepository.lagre(
            behandling.id, SykdomsvurderingForBrev(
                behandlingId = behandling.id,
                vurdering = "En vurdering",
                vurdertAv = Bruker("ident"),
            )
        )

        val resultat = hentBrevBehov(behandling)

        assertThat(resultat).isNull()
    }

    @Test
    fun `skal opprette avklaringsbehov ved revurdering`() {
        val behandling = behandling(typeBehandling = TypeBehandling.Revurdering)
        val kontekstMedPerioder = flytKontekstMedPerioder(
            behandling,
            setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )

        opprettOgLøsSykdombehov(behandling)

        steg.utfør(kontekstMedPerioder)

        val behov = hentBrevBehov(behandling)

        assertThat(behov).isNotNull
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `begrunnelse er tilstrekkelig med og uten kilder`(medKilder: Boolean) {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val kontekst = flytKontekstMedPerioder(behandling)
        opprettOgLøsSykdombehov(behandling)
        opprettOgLøsBrevbehov(behandling, kontekst)
        InMemorySykdomsvurderingForBrevRepository.lagre(
            behandling.id,
            SykdomsvurderingForBrev(
                behandling.id, "En vurdering", Bruker("Veileder"), kilder = kilder(medKilder)
            )
        )

        steg.utfør(kontekst)

        assertThat(hentBrevBehov(behandling)?.erÅpent()).isFalse()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `kilder gjør ikke blank begrunnelse tilstrekkelig`(medKilder: Boolean) {
        val behandling = behandling(TypeBehandling.Førstegangsbehandling)
        val kontekst = flytKontekstMedPerioder(behandling)
        opprettOgLøsSykdombehov(behandling)
        opprettOgLøsBrevbehov(behandling, kontekst)
        InMemorySykdomsvurderingForBrevRepository.lagre(
            behandling.id,
            SykdomsvurderingForBrev(
                behandling.id, " ", Bruker("Veileder"), kilder = kilder(medKilder)
            )
        )

        steg.utfør(kontekst)

        assertThat(hentBrevBehov(behandling)?.erÅpent()).isTrue()
    }

    private fun kilder(medKilder: Boolean): List<Dokumentkilde> =
        if (medKilder) {
            listOf(Dokumentkilde("123", "456", "Legeerklæring", LocalDate.of(2026, 1, 20)))
        } else {
            emptyList()
        }

    private fun opprettOgLøsBrevbehov(behandling: Behandling, kontekst: FlytKontekstMedPerioder) {
        steg.utfør(kontekst)
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .løsAvklaringsbehov(Definisjon.SKRIV_SYKDOMSVURDERING_BREV, "...", Bruker("Veileder"))
    }

    private fun hentBrevBehov(behandling: Behandling): Avklaringsbehov? =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentBehovForDefinisjon(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)

    private fun opprettOgLøsSykdombehov(behandling: Behandling) {
        InMemoryAvklaringsbehovRepository.opprett(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            Definisjon.AVKLAR_SYKDOM.løsesISteg,
            null,
            "..."
        )
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .løsAvklaringsbehov(Definisjon.AVKLAR_SYKDOM, "...", Bruker("meg"))
    }

    private fun flytKontekstMedPerioder(
        behandling: Behandling,
        vurderingsbehov: Set<Vurderingsbehov> = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
    ): FlytKontekstMedPerioder = no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder {
        this.behandling = behandling
        this.vurderingsbehovRelevanteForSteg = vurderingsbehov
        this.rettighetsperiode = Periode(LocalDate.now(), LocalDate.now())
    }

    private fun behandling(typeBehandling: TypeBehandling): Behandling =
        behandlingRepository.opprettBehandling(
            sakId = opprettInMemorySak().id,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            )
        )
}