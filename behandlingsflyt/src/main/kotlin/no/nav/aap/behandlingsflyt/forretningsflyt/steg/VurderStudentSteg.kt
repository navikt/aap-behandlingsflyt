package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.behandling.vilkår.student.StudentFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.student.StudentVilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
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
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        studentRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        vilkårsresultatRepository = repositoryProvider.provide(),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = Definisjon.AVKLAR_STUDENT,
            vedtakBehøverVurdering = {
                when (kontekst.vurderingType) {
                    VurderingType.FØRSTEGANGSBEHANDLING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                                (studentGrunnlag.søkerOppgirStudentstatus() || Vurderingsbehov.REVURDER_STUDENT in kontekst.vurderingsbehovRelevanteForSteg)

                    VurderingType.REVURDERING ->
                        tidligereVurderinger.muligMedRettTilAAP(kontekst, type()) &&
                                Vurderingsbehov.REVURDER_STUDENT in kontekst.vurderingsbehovRelevanteForSteg

                    VurderingType.UTVID_VEDTAKSLENGDE,
                    VurderingType.MIGRER_RETTIGHETSPERIODE,
                    VurderingType.MELDEKORT,
                    VurderingType.AUTOMATISK_BREV,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
                    VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
                    VurderingType.IKKE_RELEVANT ->
                        false
                }
            },
            erTilstrekkeligVurdert = {
                !studentGrunnlag?.vurderinger.isNullOrEmpty()
            },
            tilbakestillGrunnlag = {
                val vedtatteVurderinger = kontekst.forrigeBehandlingId
                    ?.let { studentRepository.hentHvisEksisterer(it) }
                    ?.vurderinger
                studentRepository.lagre(kontekst.behandlingId, vedtatteVurderinger)
            },
            kontekst
        )

        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                vurderStudentvilkår(kontekst)
            }

            VurderingType.UTVID_VEDTAKSLENGDE,
            VurderingType.MELDEKORT,
            VurderingType.AUTOMATISK_BREV,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT -> {
                /* noop */
            }
        }

        return Fullført
    }

    private fun vurderStudentvilkår(kontekst: FlytKontekstMedPerioder) {
        val vilkårsresultat = vilkårsresultatRepository.hent(kontekst.behandlingId)
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.STUDENT)

        val grunnlag = StudentFaktagrunnlag(
            rettighetsperiode = kontekst.rettighetsperiode,
            studentGrunnlag = studentRepository.hentHvisEksisterer(kontekst.behandlingId)
        )
        StudentVilkår(vilkårsresultat).vurder(grunnlag = grunnlag)
        vilkårsresultatRepository.lagre(kontekst.behandlingId, vilkårsresultat)
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
