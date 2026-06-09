package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentLøsningV2
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.toHumanReadable
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarStudentLøserV2(
    private val studentRepository: StudentRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarStudentLøsningV2> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
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
        val nyTidslinje = StudentGrunnlag(
            vurderinger = nyePlussVedtatte,
            oppgittStudent = null
        ).somStudenttidslinje()
        valider(kontekst.behandlingId(), nyTidslinje)

        studentRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = nyePlussVedtatte
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    private fun valider(behandlingId: BehandlingId, studentTidslinje: Tidslinje<StudentVurdering>) {
        val sykdomTidslinje = sykdomRepository.hentHvisEksisterer(behandlingId)
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()

        val inkonsistentePerioder =
            Tidslinje.map2(studentTidslinje, sykdomTidslinje) { studentVurdering, sykdomsvurdering ->
                sykdomsvurdering?.potensieltOppfyltStudent() != true && studentVurdering?.erOppfylt() == true
            }.filter { it.verdi } .perioder().toSet()

        if (inkonsistentePerioder.isNotEmpty()) {
            throw UgyldigForespørselException(
                "Vurderingene for ${inkonsistentePerioder.toHumanReadable()} stemmer ikke med periodene i § 11-5." // todo bedre feilmelding
            )
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT_V2
    }
}