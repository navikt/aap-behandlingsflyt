package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderAvslag11_27Løsning
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate.Avslag11_27VurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate.Avslag11_27VurderingerDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.help.avklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvslag11_27Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class VurderAvslag11_27LøserTest {

    private val gatewayProvider = createGatewayProvider { register<AlleAvskruddUnleash>() }
    val unleashMedAvslag1127 = FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(
            BehandlingsflytFeature.Avslag11_27
        )
    )
    private val ref1 = Kravreferanse(UUID.randomUUID())
    private val ref2 = Kravreferanse(UUID.randomUUID())

    private fun løser() = VurderAvslag11_27Løser(
        behandlingRepository = InMemoryBehandlingRepository,
        avslag11_27repository = InMemoryAvslag11_27Repository,
        unleashGateway = unleashMedAvslag1127
    )

    private fun løsning(vararg vurderinger: Avslag11_27VurderingDto) = VurderAvslag11_27Løsning(
        avslag11_27Vurdering = Avslag11_27VurderingerDto(vurderinger.toList())
    )

    private fun vurderingDto(
        referanse: Kravreferanse = ref1,
        skalAvslås: Boolean = true,
        brukersYtelse: Ytelse? = Ytelse.SYKEPENGER,
    ) = Avslag11_27VurderingDto(
        referanse = referanse.verdi.toString(),
        begrunnelse = "begrunnelse",
        harAnnenFullYtelse = skalAvslås,
        brukersYtelse = if (skalAvslås) brukersYtelse else null,
        harSykepengegrunnlagOver2G = null,
        skalAvslås1127 = skalAvslås,
    )

    @Test
    fun `lagrer ny vurdering for behandling`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 januar 2026)

        løser().løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning(vurderingDto(ref1, skalAvslås = true))
        )

        val lagret = InMemoryAvslag11_27Repository.hentHvisEksisterer(behandling.id)
        assertThat(lagret).isNotNull
        assertThat(lagret!!.vurderinger).hasSize(1)
        assertThat(lagret.vurderinger.first().skalAvslås1127).isTrue()
        assertThat(lagret.vurderinger.first().vurdertIBehandling).isEqualTo(behandling.id)
    }

    @Test
    fun `lagrer to vurderinger for ulike krav`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 januar 2026)

        løser().løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning(
                vurderingDto(ref1, skalAvslås = true),
                vurderingDto(ref2, skalAvslås = false),
            )
        )

        assertThat(InMemoryAvslag11_27Repository.hentHvisEksisterer(behandling.id)!!.vurderinger).hasSize(2)
    }

    @Test
    fun `vurdering med skalAvslås false lagres korrekt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 januar 2026)

        løser().løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning(vurderingDto(ref1, skalAvslås = false))
        )

        val vurdering = InMemoryAvslag11_27Repository.hentHvisEksisterer(behandling.id)!!.vurderinger.first()
        assertThat(vurdering.skalAvslås1127).isFalse()
        assertThat(vurdering.harAnnenFullYtelse).isFalse()
        assertThat(vurdering.brukersYtelse).isNull()
    }

    @Test
    fun `vedtatte vurderinger fra forrige behandling inkluderes`() {
        val (sak, forrigeBehandling) = opprettInMemorySakOgBehandling(1 januar 2026)
        val revurdering = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandling.id,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.MANUELL_OPPRETTELSE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.VURDER_AVSLAG_11_27))
            )
        )

        løser().løs(
            avklaringsbehovKontekst { this.behandling = forrigeBehandling },
            løsning(vurderingDto(ref1, skalAvslås = true))
        )

        løser().løs(
            avklaringsbehovKontekst { this.behandling = revurdering },
            løsning(vurderingDto(ref2, skalAvslås = false))
        )

        val lagret = InMemoryAvslag11_27Repository.hentHvisEksisterer(revurdering.id)!!
        assertThat(lagret.vurderinger).hasSize(2)
        assertThat(lagret.vurderinger.map { it.vurdertIBehandling }).containsExactlyInAnyOrder(
            revurdering.id,
            forrigeBehandling.id
        )
    }

    @Test
    fun `returnerer begrunnelse i løsningsresultat`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(1 januar 2026)

        val resultat = løser().løs(
            avklaringsbehovKontekst { this.behandling = behandling },
            løsning(vurderingDto(ref1))
        )

        assertThat(resultat.begrunnelse).isEqualTo("begrunnelse")
    }
}