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
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AndreStatligeYtelserInformasjonskrav(
    private val tidligereVurderinger: TidligereVurderinger,
    private val dagpengerGateway: DagpengerGateway,
    private val sakService: SakService,
): Informasjonskrav<AndreStatligeYtelserInformasjonskrav.DagpengerInput, AndreStatligeYtelserInformasjonskrav.DagpengerRegisterdata> {
    private val log = LoggerFactory.getLogger(javaClass)

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
        return DagpengerInput(
            person = sak.person,
            rettighetsperiode = sak.rettighetsperiode
        )
    }

    override fun hentData(input: DagpengerInput): DagpengerRegisterdata {
        val (person, rettighetsperiode) = input
        val dagpengerPerioder = dagpengerGateway.hentYtelseDagpenger(
            personidentifikatorer = person.aktivIdent().identifikator,
            fom = rettighetsperiode.fom.toString(),
            tom = rettighetsperiode.tom.toString()
        ).toSet()

        return DagpengerRegisterdata(
            dagpengerPerioder.map {
                DagpengerPeriode(
                    Periode(it.fraOgMedDato, it.tilOgMedDato),
                    it.kilde,
                    it.ytelseType
                )
            }.toSet())
    }

    override fun oppdater(
        input: DagpengerInput,
        registerdata: DagpengerRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        TODO("Not yet implemented")
    }

    data class DagpengerInput(
        val person: Person,
        val rettighetsperiode: Periode,
    ) : InformasjonskravInput

    data class DagpengerRegisterdata(
        val DagpengerPerioder: Set<DagpengerPeriode>
    ) : InformasjonskravRegisterdata

    data class DagpengerPeriode(
        val periode: Periode,
        val kilde: DagpengerKilde,
        val dagpengerYtelseType: DagpengerYtelseType
    )


    companion object : Informasjonskravkonstruktør{

        private val secureLogger = LoggerFactory.getLogger("team-logs")
        override val navn = InformasjonskravNavn.ANDRE_STATLIGE_YTELSER

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): AndreStatligeYtelserInformasjonskrav{
            return AndreStatligeYtelserInformasjonskrav(
                TidligereVurderingerImpl(repositoryProvider),
                gatewayProvider.provide(),
                SakService(repositoryProvider),
            )
        }
    }

}