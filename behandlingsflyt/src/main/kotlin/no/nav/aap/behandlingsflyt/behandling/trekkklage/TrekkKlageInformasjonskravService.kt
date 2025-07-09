package no.nav.aap.behandlingsflyt.behandling.trekkklage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.lookup.repository.RepositoryProvider

class TrekkKlageInformasjonskravService (
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val trekkKlageRepository: TrekkKlageRepository,
) : Informasjonskrav {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        trekkKlageRepository = repositoryProvider.provide(),
    )

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return ÅrsakTilBehandling.KLAGE_TRUKKET in kontekst.årsakerTilBehandling
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val vurderTrekkAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_TREKK_AV_KLAGE)

        return if (vurderTrekkAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.TRUKKET_KLAGE

        override fun konstruer(repositoryProvider: RepositoryProvider): Informasjonskrav {
            return TrekkKlageInformasjonskravService(repositoryProvider)
        }
    }
}