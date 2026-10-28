package no.nav.aap.behandlingsflyt.behandling.maksdato

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MaksdatoServiceTest {
    private val service = MaksdatoService(InMemoryArenaMaksdatoRepository)

    @BeforeEach
    fun resetRepository() {
        InMemoryArenaMaksdatoRepository.reset()
    }

    @Test
    fun `uten grunnlag returneres tom liste`() {
        assertThat(service.hentMaksdatoer(BehandlingId(1L))).isEmpty()
    }

    @Test
    fun `grunnlag uten Arena-maksdato returnerer tom liste`() {
        val behandlingId = BehandlingId(2L)
        InMemoryArenaMaksdatoRepository.lagre(behandlingId, null)

        assertThat(service.hentMaksdatoer(behandlingId)).isEmpty()
    }

    @Test
    fun `Arena-grunnlag mappes til maksdato`() {
        val behandlingId = BehandlingId(3L)
        val arenaMaksdato = ArenaMaksdato(
            arenaSaksnummer = "ARENA-123",
            status = "AKTIV",
            maksdato = LocalDate.of(2026, 5, 31),
            vedtakTil = LocalDate.of(2026, 6, 30),
        )
        InMemoryArenaMaksdatoRepository.lagre(behandlingId, arenaMaksdato)

        assertThat(service.hentMaksdatoer(behandlingId)).containsExactly(
            Maksdato(
                kilde = MaksdatoKilde.ARENA,
                saksnummer = "ARENA-123",
                status = "AKTIV",
                maksdato = LocalDate.of(2026, 5, 31),
                vedtakTil = LocalDate.of(2026, 6, 30),
            )
        )
    }
}
