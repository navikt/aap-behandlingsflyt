package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.lookup.repository.RepositoryProvider

/* Burde flytorkestratoren oppdage at det har kommet en ny årsak til behandling,
 * og derfor tilbakeføre aktivt steg til første steg som har denne årsaken knyttet
 * til seg?
 */
class TrukketSøknadService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trukketSøknadRepository: TrukketSøknadRepository,
) : Informasjonskrav {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        trukketSøknadRepository = repositoryProvider.provide(),
    )

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return Vurderingsbehov.SØKNAD_TRUKKET in kontekst.vurderingsbehovRelevanteForSteg
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val vurderTrekkAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_TREKK_AV_SØKNAD)

        return if (vurderTrekkAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }

    fun søknadErTrukket(behandlingId: BehandlingId): Boolean {
        return trukketSøknadRepository.hentTrukketSøknadVurderinger(behandlingId).isNotEmpty()
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.TRUKKET_SØKNAD

        override fun konstruer(repositoryProvider: RepositoryProvider): Informasjonskrav {
            return TrukketSøknadService(repositoryProvider)
        }
    }
}