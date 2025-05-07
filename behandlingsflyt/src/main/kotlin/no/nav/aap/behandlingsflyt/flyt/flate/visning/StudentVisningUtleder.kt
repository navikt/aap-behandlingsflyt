package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider


@Suppress("unused")
class StudentVisningUtleder(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val studentRepository: StudentRepository,
) : StegGruppeVisningUtleder {

    constructor(repositoryProvider: RepositoryProvider): this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
    )

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId)
        if (studentGrunnlag?.studentvurdering != null) {
            return true
        }
        val hentAvklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        return hentAvklaringsbehovene
            .hentBehovForDefinisjon(Definisjon.AVKLAR_STUDENT)?.erIkkeAvbrutt() == true
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.STUDENT
    }
}