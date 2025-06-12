package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Avslått
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class KlagebehandlingNayOppsummeringSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
    private val trekkKlageService: TrekkKlageService,
    private val klageresultatUtleder: KlageresultatUtleder
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val resultat = klageresultatUtleder.utledKlagebehandlingResultat(kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        if (resultat is Avslått) {
            avklaringsbehov.avbrytForSteg(KlagebehandlingNaySteg.Companion.type())
            return Fullført
        }

        if(trekkKlageService.klageErTrukket(kontekst.behandlingId)) {
            avklaringsbehov.avbrytForSteg(type())
            return Fullført
        }

        val behandlendeEnhetVurdering = behandlendeEnhetRepository.hentHvisEksisterer(kontekst.behandlingId)?.vurdering
        requireNotNull(behandlendeEnhetVurdering) {
            "Behandlende enhet skal være satt"
        }
        return if (behandlendeEnhetVurdering.skalBehandlesAvBådeNavKontorOgNay()) {
            if (!erAlleredeVurdert(avklaringsbehov)) {
                FantAvklaringsbehov(Definisjon.BEKREFT_TOTALVURDERING_KLAGE)
            } else {
                return Fullført
            }
        } else {
            avklaringsbehov.avbrytForSteg(type())
            Fullført
        }
    }

    private fun erAlleredeVurdert(avklaringsbehov: Avklaringsbehovene): Boolean =
        avklaringsbehov.erVurdertTidligereIBehandlingen(
            Definisjon.BEKREFT_TOTALVURDERING_KLAGE
        )

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return KlagebehandlingNayOppsummeringSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                TrekkKlageService(repositoryProvider),
                KlageresultatUtleder(repositoryProvider)
            )
        }

        override fun type(): StegType {
            return StegType.KLAGEBEHANDLING_OPPSUMMERING
        }
    }

    private fun Avklaringsbehovene.harIkkeBlittLøst(definisjon: Definisjon): Boolean {
        return this.alle()
            .filter { it.definisjon == definisjon }
            .none { it.status() == Status.AVSLUTTET }
    }
}