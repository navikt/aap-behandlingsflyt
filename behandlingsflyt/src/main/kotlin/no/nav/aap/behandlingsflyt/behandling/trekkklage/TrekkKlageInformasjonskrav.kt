package no.nav.aap.behandlingsflyt.behandling.trekkklage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.IngenRegisterData
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class TrekkKlageInformasjonskrav(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : Informasjonskrav<IngenInput, IngenRegisterData> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return Vurderingsbehov.KLAGE_TRUKKET in kontekst.vurderingsbehovRelevanteForSteg
    }

    override fun klargjør(kontekst: FlytKontekstMedPerioder) = IngenInput

    override fun hentData(input: IngenInput) = IngenRegisterData

    override fun oppdater(
        input: IngenInput,
        registerdata: IngenRegisterData,
        kontekst: FlytKontekstMedPerioder
    ): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val vurderTrekkAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_TREKK_AV_KLAGE)

        return if (vurderTrekkAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.TRUKKET_KLAGE

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): Informasjonskrav<IngenInput, IngenRegisterData> {
            return TrekkKlageInformasjonskrav(repositoryProvider)
        }
    }
}