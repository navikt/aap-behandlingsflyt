package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarStudentLøser(
    private val behandlingRepository: BehandlingRepository,
    private val studentRepository: StudentRepository,
) : AvklaringsbehovsLøser<AvklarStudentLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarStudentLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        studentRepository.lagre(
            behandlingId = behandling.id,
            studentvurdering = løsning.studentvurdering.let { StudentVurdering(
                id = it.id,
                begrunnelse = it.begrunnelse,
                harAvbruttStudie = it.harAvbruttStudie,
                godkjentStudieAvLånekassen = it.godkjentStudieAvLånekassen,
                avbruttPgaSykdomEllerSkade = it.avbruttPgaSykdomEllerSkade,
                harBehovForBehandling = it.harBehovForBehandling,
                avbruttStudieDato = it.avbruttStudieDato,
                avbruddMerEnn6Måneder = it.avbruddMerEnn6Måneder,
                vurdertAv = kontekst.bruker.ident,
            ) }
        )

        return LøsningsResultat(
            begrunnelse = løsning.studentvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT
    }
}