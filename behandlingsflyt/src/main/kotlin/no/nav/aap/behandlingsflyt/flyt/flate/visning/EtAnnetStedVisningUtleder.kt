package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

// Er ikke ubrukt, men blir opprettet med refleksjon
@Suppress("unused")
class EtAnnetStedVisningUtleder(
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
) : StegGruppeVisningUtleder {
    constructor(repositoryProvider: RepositoryProvider): this(
        institusjonsoppholdRepository = repositoryProvider.provide(),
    )


    override fun skalVises(behandlingId: BehandlingId): Boolean {
        return !institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)?.oppholdene?.opphold.isNullOrEmpty()
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.ET_ANNET_STED
    }
}