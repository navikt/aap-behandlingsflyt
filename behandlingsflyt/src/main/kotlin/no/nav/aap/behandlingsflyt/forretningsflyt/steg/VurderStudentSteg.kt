package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.søkerOppgirStudentstatus
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderStudentSteg private constructor(
    private val studentRepository: StudentRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_STUDENT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                                studentGrunnlag.søkerOppgirStudentstatus()

                    VurderingType.REVURDERING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                                Vurderingsbehov.REVURDER_STUDENT in kontekst.vurderingsbehovRelevanteForSteg

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                studentGrunnlag?.studentvurdering != null
            },
            tilbakestillGrunnlag = {
                val vedtattVurdering = kontekst.forrigeBehandlingId
                    ?.let { studentRepository.hentHvisEksisterer(it) }
                    ?.studentvurdering
                studentRepository.lagre(kontekst.behandlingId, vedtattVurdering)
            },
            kontekst
        )
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VurderStudentSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_STUDENT
        }
    }
}
