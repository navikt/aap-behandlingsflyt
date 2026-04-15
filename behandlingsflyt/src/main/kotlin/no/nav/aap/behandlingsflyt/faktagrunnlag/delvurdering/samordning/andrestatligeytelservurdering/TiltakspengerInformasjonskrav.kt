package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSisteKalenderdagForBehandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.tiltakspenger.TiltakspengerRepository
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class TiltakspengerInformasjonskrav(
    private val tidligereVurderinger: TidligereVurderinger,
    private val tiltakspengerGateway: TiltakspengerGateway,
    private val tiltakspengerRepository: TiltakspengerRepository,
    private val sakService: SakService,
    private val unleashGateway: UnleashGateway
) : Informasjonskrav<TiltakspengerInformasjonskrav.TiltakspengerInput, TiltakspengerInformasjonskrav.TiltakspengerRegisterdata> {
    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() && !tidligereVurderinger.girAvslagEllerIngenBehandlingsgrunnlag(
            kontekst, steg
        ) && (oppdatert.ikkeKjørtSisteKalenderdagForBehandling(kontekst.behandlingId) || kontekst.rettighetsperiode != oppdatert?.rettighetsperiode || kontekst.erVurderingsbehovEndretEtterOppdatertInformasjonskrav(
            oppdatert
        ))
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder): TiltakspengerInput {
        val sak = sakService.hentSakFor(kontekst.behandlingId)
        val eksisterendeData = tiltakspengerRepository.hent(kontekst.behandlingId)
        return TiltakspengerInput(
            person = sak.person,
            rettighetsperiode = sak.rettighetsperiode,
            eksisterendeData = eksisterendeData.toSet()
        )
    }

    override fun hentData(input: TiltakspengerInput): TiltakspengerRegisterdata {
        val (person, rettighetsperiode) = input
        val tiltakspengerPerioder = tiltakspengerGateway.hentYtelseTiltakspenger(
            personidentifikatorer = person.aktivIdent().identifikator,
            fom = rettighetsperiode.fom,
            tom = rettighetsperiode.tom
        ).toSet()

        return TiltakspengerRegisterdata(
            tiltakspengerPerioder
        )
    }

    override fun oppdater(
        input: TiltakspengerInput,
        registerdata: TiltakspengerRegisterdata,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val (tiltakspenger) = registerdata

        return if (harEndringerITiltakspenger(input.eksisterendeData, tiltakspenger)) {
            tiltakspengerRepository.lagre(
                kontekst.behandlingId,
                tiltakspenger.toList()
            )
            Informasjonskrav.Endret.ENDRET
        } else {
            Informasjonskrav.Endret.IKKE_ENDRET
        }

    }

    fun harEndringerITiltakspenger(
        eksisterendeData: Set<TiltakspengerPeriode>?,
        tiltakspenger: Set<TiltakspengerPeriode>
    ): Boolean {
        return eksisterendeData == null || eksisterendeData != tiltakspenger
    }

    data class TiltakspengerInput(
        val person: Person,
        val rettighetsperiode: Periode,
        val eksisterendeData: Set<TiltakspengerPeriode>? = null
    ) : InformasjonskravInput

    data class TiltakspengerRegisterdata(
        val tiltakspengerPerioder: Set<TiltakspengerPeriode>
    ) : InformasjonskravRegisterdata


    companion object : Informasjonskravkonstruktør {

        override val navn = InformasjonskravNavn.TILTAKSPENGER

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): TiltakspengerInformasjonskrav {
            return TiltakspengerInformasjonskrav(
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
                tiltakspengerGateway = gatewayProvider.provide(),
                tiltakspengerRepository = repositoryProvider.provide(),
                sakService = SakService(repositoryProvider, gatewayProvider),
                unleashGateway = gatewayProvider.provide()
            )
        }
    }
}