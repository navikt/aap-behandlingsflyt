package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarStudentLøsningV2
import no.nav.aap.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.steg.student.StudentRepository
import no.nav.aap.student.StudentValidering
import no.nav.aap.student.StudentVurdering
import no.nav.aap.behandlingsflyt.steg.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandling.BehandlingId
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
        validerPerioder(kontekst.behandlingId(), nyTidslinje)

        studentRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = nyePlussVedtatte
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    private fun validerPerioder(behandlingId: BehandlingId, studentTidslinje: Tidslinje<StudentVurdering>) {
        val sykdomTidslinje = sykdomRepository.hentHvisEksisterer(behandlingId)
            ?.somSykdomsvurderingstidslinje()
            .orEmpty()

        val ugyldigePerioder =
            StudentValidering.nårVurderingErKonsistentMedSykdom(studentTidslinje, sykdomTidslinje).filter { !it.verdi }
                .perioder().toSet()

        if (ugyldigePerioder.isNotEmpty()) {
            throw UgyldigForespørselException(
                "Vurderingene for ${ugyldigePerioder.toHumanReadable()} stemmer ikke med periodene i § 11-5." // todo bedre feilmelding
            )
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT_V2
    }
}