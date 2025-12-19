package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStudentEnkelLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class AvklarStudentLøser(
    private val behandlingRepository: BehandlingRepository,
    private val studentRepository: StudentRepository,
    private val sakRepository: SakRepository
) : AvklaringsbehovsLøser<AvklarStudentEnkelLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarStudentEnkelLøsning
    ): LøsningsResultat {
        val løsninger = løsning.løsningerForPerioder ?: listOf(løsning.studentvurdering)
        
        if (løsninger.size != 1) {
            throw IllegalArgumentException("Støtter kun én vurdering per løsning")
        }
        
        val sak = sakRepository.hent(kontekst.kontekst.sakId)

        val forrigeBehandlingId = kontekst.kontekst.forrigeBehandlingId

        val forrigeVedtatteGrunnlag = forrigeBehandlingId
            ?.let { studentRepository.hentHvisEksisterer(it) }
        
        val vedtatteVurderinger = forrigeVedtatteGrunnlag?.vurderinger.orEmpty()

        val nyeVurderinger = løsninger.map {
            it.tilStudentVurdering(
                kontekst.bruker,
                kontekst.behandlingId(),
                sak.rettighetsperiode.fom
            )
        }.toSet()
        
        val nyePlussVedtatte = nyeVurderinger + vedtatteVurderinger
        
        studentRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            vurderinger = nyeVurderinger // TODO: Lagre nye + vedtatte
        )

        return LøsningsResultat(
            begrunnelse = løsning.studentvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STUDENT
    }
}