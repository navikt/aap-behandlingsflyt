package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.StegStatus
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FrivilligeAvklaringsbehovTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository

    @Test
    fun `skal få frem frivillige avklaringsbehov mellom aktivt steg og start`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandling.id)

        behandling.oppdaterSteg(StegTilstand(stegStatus = StegStatus.START, stegType = StegType.VURDER_SYKEPENGEERSTATNING, aktiv = true))

        assertThat(avklaringsbehovene.allePlussFrivillige(behandling)).isNotEmpty
    }
}