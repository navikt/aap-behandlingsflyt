package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dagpenger.DagpengerRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class DagpengerInformasjonskrav(
    private val tidligereVurderinger: TidligereVurderinger,
    private val dagpengerGateway: DagpengerGateway,
    private val dagpengerRepository: DagpengerRepository,
    private val sakService: SakService,
): Informasjonskrav<DagpengerInformasjonskrav.DagpengerInput, DagpengerInformasjonskrav.DagpengerRegisterdata> {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
            kontekst, steg
        ) && (oppdatert.ikkeKjørtSisteKalenderdag() || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode)
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder): DagpengerInput {
        val sak = sakService.hentSakFor(kontekst.behandlingId)
        val eksisterendeData = dagpengerRepository.hent(kontekst.behandlingId)
        return DagpengerInput(
            person = sak.person,
            rettighetsperiode = sak.rettighetsperiode,
            eksisterendeData = eksisterendeData.toSet()
        )
    }

    override fun hentData(input: DagpengerInput): DagpengerRegisterdata {
        val (person, rettighetsperiode) = input
        val dagpengerPerioder = dagpengerGateway.hentYtelseDagpenger(
            personidentifikatorer = person.aktivIdent().identifikator,
            fom = rettighetsperiode.fom,
            tom = rettighetsperiode.tom
        ).toSet()

        return DagpengerRegisterdata(
            dagpengerPerioder
        )
    }

    override fun oppdater(
        input: DagpengerInput,
        registerdata: DagpengerRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val (dagpenger) = registerdata

        return if (harEndringerIDagpenger(input.eksisterendeData, dagpenger)) {
            dagpengerRepository.lagre(
                kontekst.behandlingId,
                dagpenger.toList()
            )
            Informasjonskrav.Endret.ENDRET
        } else {
            Informasjonskrav.Endret.IKKE_ENDRET
        }

    }

    fun harEndringerIDagpenger(
        eksisterendeData: Set<DagpengerPeriode>?,
        dagpenger: Set<DagpengerPeriode>
    ): Boolean {
        return eksisterendeData == null || eksisterendeData != dagpenger
    }


    data class DagpengerInput(
        val person: Person,
        val rettighetsperiode: Periode,
        val eksisterendeData: Set<DagpengerPeriode>? = null
    ) : InformasjonskravInput

    data class DagpengerRegisterdata(
        val DagpengerPerioder: Set<DagpengerPeriode>
    ) : InformasjonskravRegisterdata


    companion object : Informasjonskravkonstruktør{

        private val secureLogger = LoggerFactory.getLogger("team-logs")
        override val navn = InformasjonskravNavn.DAGPENGER

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): DagpengerInformasjonskrav{
            return DagpengerInformasjonskrav(
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                SakService(repositoryProvider),
            )
        }
    }

}