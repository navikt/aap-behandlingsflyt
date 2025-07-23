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
        val harRegisterBarn =
            barnRepository.hentHvisEksisterer(behandlingId)?.registerbarn?.barn.orEmpty().isNotEmpty()
        if (harRegisterBarn) {
            return true
        }
        return barnRepository.hentHvisEksisterer(behandlingId)?.oppgitteBarn?.oppgitteBarn.orEmpty().isNotEmpty()
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.BARNETILLEGG
    }
}
