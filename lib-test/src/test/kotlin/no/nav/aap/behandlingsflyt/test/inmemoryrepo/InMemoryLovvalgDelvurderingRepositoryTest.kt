package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InMemoryLovvalgDelvurderingRepositoryTest {
    private val repository = InMemoryLovvalgDelvurderingRepository
    private val vurdering = KanBehandlesAutomatiskVurdering(true, emptyList(), Periode(LocalDate.now(), Tid.MAKS))

    @Test
    fun `kan hente aktiv og historisk vurdering etter id`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        repository.lagre(behandling.id, vurdering, vurdering.periode.fom)
        val opprinnelig = requireNotNull(repository.hentHvisEksisterer(behandling.id))
        repository.lagre(behandling.id, vurdering.copy(kanBehandlesAutomatisk = false), vurdering.periode.fom)
        val ny = requireNotNull(repository.hentHvisEksisterer(behandling.id))

        assertThat(ny.delvurderingId).isNotEqualTo(opprinnelig.delvurderingId)
        assertThat(repository.hentBasertPåDelvurderingId(opprinnelig.delvurderingId)).isEqualTo(opprinnelig)
        assertThat(repository.hentBasertPåDelvurderingId(ny.delvurderingId)).isEqualTo(ny)
        assertThat(repository.hentBasertPåDelvurderingId(-1L)).isNull()

        repository.slett(behandling.id)
        assertThat(repository.hentHvisEksisterer(behandling.id)).isNull()
        assertThat(repository.hentBasertPåDelvurderingId(opprinnelig.delvurderingId)).isNull()
        assertThat(repository.hentBasertPåDelvurderingId(ny.delvurderingId)).isNull()
    }

    @Test
    fun `kopiering og uendret lagring beholder samme id`() {
        val (_, fra) = opprettInMemorySakOgBehandling()
        val (_, til) = opprettInMemorySakOgBehandling()
        repository.lagre(fra.id, vurdering, vurdering.periode.fom)
        val opprinnelig = requireNotNull(repository.hentHvisEksisterer(fra.id))
        repository.kopier(fra.id, til.id)
        repository.lagre(til.id, vurdering, vurdering.periode.fom)
        assertThat(repository.hentHvisEksisterer(til.id)).isEqualTo(opprinnelig)

        repository.lagre(til.id, vurdering.copy(kanBehandlesAutomatisk = false), vurdering.periode.fom)
        assertThat(repository.hentHvisEksisterer(fra.id)).isEqualTo(opprinnelig)
        repository.slett(fra.id)
        assertThat(repository.hentBasertPåDelvurderingId(opprinnelig.delvurderingId)).isEqualTo(opprinnelig)

        repository.slett(til.id)
        assertThat(repository.hentBasertPåDelvurderingId(opprinnelig.delvurderingId)).isNull()
    }
}
