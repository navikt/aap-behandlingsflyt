package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderRettighetsperiodeService(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
): Informasjonskrav {
    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val relevantAvklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_RETTIGHETSPERIODE)

        return if (relevantAvklaringsbehov == null)
            ENDRET
        else
            IKKE_ENDRET
    }

    companion object: Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.RETTIGHETSPERIODE

        override fun erRelevant(kontekst: FlytKontekstMedPerioder, oppdatert: InformasjonskravOppdatert?): Boolean {
            return ÅrsakTilBehandling.VURDER_RETTIGHETSPERIODE in kontekst.vurdering.årsakerTilBehandling
        }

        override fun konstruer(connection: DBConnection): Informasjonskrav {
            return VurderRettighetsperiodeService(RepositoryProvider(connection).provide())
        }
    }
}