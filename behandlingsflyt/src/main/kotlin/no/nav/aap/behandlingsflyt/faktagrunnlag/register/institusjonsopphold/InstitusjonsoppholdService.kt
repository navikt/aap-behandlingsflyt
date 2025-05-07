package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.adapter.InstitusjonsoppholdGateway
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Duration
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGateway as IInstitusjonsoppholdGateway

class InstitusjonsoppholdService private constructor(
    private val sakService: SakService,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val institusjonsoppholdRegisterGateway: IInstitusjonsoppholdGateway,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val behandlingId = kontekst.behandlingId
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val sak = sakService.hent(kontekst.sakId)

        val rettighetsperiode = sak.rettighetsperiode

        val institusjonsopphold = institusjonsoppholdRegisterGateway.innhent(sak.person)
            .filter { it.periode().overlapper(rettighetsperiode) }

        institusjonsoppholdRepository.lagreOpphold(behandlingId, institusjonsopphold)

        return if (erUendret(eksisterendeGrunnlag, hentHvisEksisterer(behandlingId))) IKKE_ENDRET else ENDRET
    }

    private fun erUendret(
        eksisterendeGrunnlag: InstitusjonsoppholdGrunnlag?,
        institusjonsopphold: InstitusjonsoppholdGrunnlag?
    ): Boolean {
        return eksisterendeGrunnlag?.oppholdene?.opphold == institusjonsopphold?.oppholdene?.opphold
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): InstitusjonsoppholdGrunnlag? {
        return institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.INSTITUSJONSOPPHOLD

        override fun konstruer(repositoryProvider: RepositoryProvider): InstitusjonsoppholdService {
            val sakRepository = repositoryProvider.provide<SakRepository>()
            return InstitusjonsoppholdService(
                SakService(sakRepository),
                repositoryProvider.provide(),
                InstitusjonsoppholdGateway,
                TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }
}