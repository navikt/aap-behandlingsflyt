package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrivilligeAvklaringsbehovTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

    @Test
    fun `skal få frem frivillige avklaringsbehov mellom aktivt steg og start`() {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, BehandlingId(133L))
        val frivilligeAvklaringsbehov =
            FrivilligeAvklaringsbehov(avklaringsbehovene, Førstegangsbehandling.flyt(), StegType.VURDER_SYKEPENGEERSTATNING)

        assertThat(frivilligeAvklaringsbehov.alle()).isNotEmpty
    }
}