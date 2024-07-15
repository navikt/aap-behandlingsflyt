package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class BehandlingReferanseServiceTest {
    @Test
    fun `kaster NoSuchElementException hvis behandling ikke funnet`() {
        val behandlingRepository = mockk<BehandlingRepository>()

        val service = BehandlingReferanseService(behandlingRepository)

        every { behandlingRepository.hent(any(UUID::class)) } throws NoSuchElementException()

        assertThrows<ElementNotFoundException> { service.behandling(BehandlingReferanse(UUID.randomUUID().toString())) }
    }
}