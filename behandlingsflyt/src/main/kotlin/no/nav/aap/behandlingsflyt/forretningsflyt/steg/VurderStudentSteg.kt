package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.søkerOppgirStudentstatus
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.oppdaterAvklaringsbehov
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg.Companion.type
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderStudentSteg private constructor(
    private val studentRepository: StudentRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (Miljø.erProd()) {
            return gammelAdferd(kontekst)
        }

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

        oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_STUDENT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING,
                    VurderingType.REVURDERING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                                studentGrunnlag.søkerOppgirStudentstatus()

                    VurderingType.MELDEKORT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
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
        )
        return Fullført
    }

    private fun gammelAdferd(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
                    avklaringsbehovService.avbrytForSteg(kontekst.behandlingId, type())
                    return Fullført
                }

                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
                if (studentGrunnlag != null && !studentGrunnlag.erKonsistent()) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_STUDENT)
                }
            }

            VurderingType.REVURDERING -> {
                val studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)
                if (studentGrunnlag != null && !studentGrunnlag.erKonsistent()) {
                    return FantAvklaringsbehov(Definisjon.AVKLAR_STUDENT)
                }
            }

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.IKKE_RELEVANT -> {
                // Do nothing
            }
        }
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
