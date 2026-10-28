package no.nav.aap.behandlingsflyt.behandling.maksdato

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class MaksdatoService(private val arenaMaksdatoRepository: ArenaMaksdatoRepository) {
    constructor(repositoryProvider: RepositoryProvider) : this(repositoryProvider.provide())

    fun hentMaksdatoer(behandlingId: BehandlingId): List<Maksdato> {
        val arenaMaksdato = arenaMaksdatoRepository.hentHvisEksisterer(behandlingId)?.arenaMaksdato
        val arenaMaksdatoer = arenaMaksdato?.let {
            listOf(
                Maksdato(
                    kilde = MaksdatoKilde.ARENA,
                    saksnummer = it.arenaSaksnummer,
                    status = it.status,
                    maksdato = it.maksdato,
                    vedtakTil = it.vedtakTil,
                )
            )
        }.orEmpty()

        // TODO: Legg til maksdato fra Kelvin-saker når det er behov
        return arenaMaksdatoer
    }
}
