package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.vilkårIkkeOppfylt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSykdomSteg private constructor(
    private val studentRepository: StudentRepository,
    private val sykdomRepository: SykdomRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId),
            definisjon = Definisjon.AVKLAR_SYKDOM,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { tilstrekkeligVurdert(kontekst) },
            tilbakestillGrunnlag = {
                val vedtatteSykdomsvurderinger = kontekst.forrigeBehandlingId
                    ?.let { sykdomRepository.hentHvisEksisterer(it) }
                    ?.sykdomsvurderinger
                    ?: emptyList()
                sykdomRepository.lagre(kontekst.behandlingId, vedtatteSykdomsvurderinger)
            },
            kontekst
        )
        return Fullført
    }

    fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {

        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING,
            VurderingType.REVURDERING -> {
                /* Hvordan håndtere periodisering av studentGrunnlag? */
                val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

                tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                        studentGrunnlag.vilkårIkkeOppfylt() &&
                        kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
            }

            VurderingType.MELDEKORT -> false
            VurderingType.IKKE_RELEVANT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT -> false
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9 -> false
        }
    }

    fun tilstrekkeligVurdert(kontekst: FlytKontekstMedPerioder): Boolean {
        val sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(kontekst.behandlingId)
        return sykdomGrunnlag != null && sykdomGrunnlag.sykdomsvurderinger.isNotEmpty()
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): BehandlingSteg {
            return VurderSykdomSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.AVKLAR_SYKDOM
        }
    }
}
