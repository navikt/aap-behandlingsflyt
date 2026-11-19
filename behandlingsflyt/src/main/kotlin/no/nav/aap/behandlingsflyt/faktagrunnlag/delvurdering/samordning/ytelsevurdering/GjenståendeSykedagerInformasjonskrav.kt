package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravRegisterdata
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class GjenståendeSykedagerInformasjonskrav(
    private val sakRepository: SakRepository,
    private val gjenståendeSykedagerRepository: GjenståendeSykedagerRepository,
    private val sykepengerGateway: SykepengerGateway,
    private val unleashGateway: UnleashGateway,
) : Informasjonskrav<GjenståendeSykedagerInformasjonskrav.Input, GjenståendeSykedagerInformasjonskrav.Registerdata> {
    private val log = LoggerFactory.getLogger(javaClass)

    data class Input(val personIdent: String) : InformasjonskravInput

    data class Registerdata(val gjenståendeSykedager: GjenståendeSykedager?) : InformasjonskravRegisterdata

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?,
    ): Boolean = kontekst.vurderingType == VurderingType.FØRSTEGANGSBEHANDLING
            && (oppdatert == null || oppdatert.behandlingId != kontekst.behandlingId)
            && unleashGateway.isEnabled(BehandlingsflytFeature.MaksdatoSykepenger)

    override fun klargjør(kontekst: FlytKontekstMedPerioder): Input {
        val sak = sakRepository.hent(kontekst.sakId)
        return Input(sak.person.aktivIdent().identifikator)
    }

    // Informasjonen er ikke kritisk for behandlingen, så feil fra sykepengetjenesten skal ikke stoppe flyten.
    @Suppress("TooGenericExceptionCaught")
    override fun hentData(input: Input): Registerdata {
        return try {
            Registerdata(sykepengerGateway.hentGjenståendeSykedager(input.personIdent))
        } catch (e: Exception) {
            log.warn("Klarte ikke hente gjenstående sykedager", e)
            Registerdata(null)
        }
    }

    override fun oppdater(
        input: Input,
        registerdata: Registerdata,
        kontekst: FlytKontekstMedPerioder,
    ): Informasjonskrav.Endret {
        val gjenståendeSykedager = registerdata.gjenståendeSykedager ?: return Informasjonskrav.Endret.IKKE_ENDRET

        val eksisterende = gjenståendeSykedagerRepository.hentHvisEksisterer(kontekst.behandlingId)
        if (eksisterende == gjenståendeSykedager) {
            return Informasjonskrav.Endret.IKKE_ENDRET
        }

        gjenståendeSykedagerRepository.lagre(kontekst.behandlingId, gjenståendeSykedager)
        return Informasjonskrav.Endret.ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.GJENSTAAENDE_SYKEDAGER

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): GjenståendeSykedagerInformasjonskrav = GjenståendeSykedagerInformasjonskrav(
            sakRepository = repositoryProvider.provide(),
            gjenståendeSykedagerRepository = repositoryProvider.provide(),
            sykepengerGateway = gatewayProvider.provide(),
            unleashGateway = gatewayProvider.provide(),
        )
    }
}
