package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime

class InMemoryArenaMigreringRepositoryTest {
    private val repository = InMemoryArenaMigreringRepository

    @Test
    fun `slett med førstegangsbehandlingen fjerner migreringen for saken`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        repository.lagre(migrering(sak))

        repository.slett(behandling.id)
        repository.slett(behandling.id)

        assertThat(repository.hentForSakHvisEksisterer(sak.id)).isNull()
    }

    @Test
    fun `slett med en klagebehandling beholder migreringen`() {
        val (sak, _) = opprettInMemorySakOgBehandling()
        val klage = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTATT_KLAGE)),
                årsak = ÅrsakTilOpprettelse.KLAGE,
            ),
        )
        repository.lagre(migrering(sak))

        repository.slett(klage.id)

        assertThat(repository.hentForSakHvisEksisterer(sak.id)).isNotNull
    }

    @Test
    fun `slett med ukjent behandling gjør ingenting`() {
        assertDoesNotThrow { repository.slett(BehandlingId(Long.MAX_VALUE)) }
    }

    private fun migrering(sak: Sak) = ArenaMigrering(
        sakId = sak.id,
        saksnummerArena = "2018-123456",
        ident = sak.person.aktivIdent().identifikator,
        migrertTidspunkt = LocalDateTime.now(),
    )
}
