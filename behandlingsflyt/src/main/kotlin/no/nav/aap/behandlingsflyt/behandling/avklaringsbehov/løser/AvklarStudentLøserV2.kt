package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsningV2
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarStudentLøserV2(
    private val studentRepository: StudentRepository,
) : AvklaringsbehovsLøser<AvklarStudentLøsningV2> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarStudentLøsningV2
    ): LøsningsResultat {
        val nyeVurderinger = (
                løsning.løsningerForPerioder
                    .map {
                        it.tilStudentVurdering(
                            kontekst.bruker,
                            kontekst.behandlingId(),
                        )
                    }
                ).toSet()


        val forrigeBehandlingId = kontekst.kontekst.forrigeBehandlingId

        val forrigeVedtatteGrunnlag = forrigeBehandlingId
            ?.let { studentRepository.hentHvisEksisterer(it) }

        val vedtatteVurderinger = forrigeVedtatteGrunnlag?.vurderinger.orEmpty()

        val nyePlussVedtatte = nyeVurderinger + vedtatteVurderinger

        studentRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = nyePlussVedtatte
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT_V2
    }
}