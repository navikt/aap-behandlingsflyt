package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InMemorySamordningYtelseRepositoryTest {
    @Test
    fun `lagre og hente ut igjen`() {
        val repo = InMemorySamordningYtelseRepository
        repo.lagre(BehandlingId(123), emptyList())

        val res = repo.hentHvisEksisterer(BehandlingId(123))

        assertThat(res).isNotNull()
    }
}