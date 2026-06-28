package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvslag11_27Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class Avslag11_27VisningUtlederTest {

    @BeforeEach
    fun reset() {
        InMemoryAvslag11_27Repository.reset()
    }

    private fun utleder() = Avslag11_27VisningUtleder(inMemoryRepositoryProvider)

    @Test
    fun `gruppe er AVSLAG_11_27`() {
        assertThat(utleder().gruppe()).isEqualTo(StegGruppe.AVSLAG_11_27)
    }

    @Test
    fun `skalVises er false når ingen grunnlag og ingen avklaringsbehov`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        assertThat(utleder().skalVises(behandling.id)).isFalse()
    }

    @Test
    fun `skalVises er true når grunnlag med vurderinger finnes`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val ref = Kravreferanse(UUID.randomUUID())

        InMemoryAvslag11_27Repository.lagre(
            behandling.id, listOf(
                Avslag11_27Vurdering(
                    referanse = ref,
                    begrunnelse = "b",
                    harAnnenFullYtelse = true,
                    brukersYtelse = Ytelse.SYKEPENGER,
                    harSykepengegrunnlagOver2G = null,
                    skalAvslås1127 = true,
                    vurdertIBehandling = behandling.id,
                    vurdertTidspunkt = Instant.now(),
                    vurdertAv = Bruker("test"),
                )
            )
        )

        assertThat(utleder().skalVises(behandling.id)).isTrue()
    }

    @Test
    fun `skalVises er true når avklaringsbehov VURDER_AVSLAG_11_27 er aktivt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id).leggTil(
            definisjon = Definisjon.VURDER_AVSLAG_11_27,
            funnetISteg = StegType.VURDER_AVSLAG_11_27,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
        )

        assertThat(utleder().skalVises(behandling.id)).isTrue()
    }

    @Test
    fun `skalVises er false når avklaringsbehov er avbrutt`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val behovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        behovene.leggTil(
            definisjon = Definisjon.VURDER_AVSLAG_11_27,
            funnetISteg = StegType.VURDER_AVSLAG_11_27,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
        )
        behovene.avbryt(Definisjon.VURDER_AVSLAG_11_27)

        assertThat(utleder().skalVises(behandling.id)).isFalse()
    }
}