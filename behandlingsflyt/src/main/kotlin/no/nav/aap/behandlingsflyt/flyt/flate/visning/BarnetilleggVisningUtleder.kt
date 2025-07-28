package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

@Suppress("unused")
class BarnetilleggVisningUtleder(
    private val barnRepository: BarnRepository,
) : StegGruppeVisningUtleder {

    constructor(repositoryProvider: RepositoryProvider) : this(
        barnRepository = repositoryProvider.provide()
    )


    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val harRegsiterBarn =
            barnRepository.hentHvisEksisterer(behandlingId)?.registerbarn?.identer?.isNotEmpty() == true
        if (harRegsiterBarn) {
            return true
        }
        return barnRepository.hentHvisEksisterer(behandlingId)?.oppgitteBarn?.oppgitteBarn?.isNotEmpty() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.BARNETILLEGG
    }
}