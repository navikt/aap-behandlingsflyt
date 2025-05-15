package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.TilbakeføresFraKvalitetsikrer
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.lookup.repository.RepositoryProvider

class KvalitetssikringsSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider) : this(
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        if (kontekst.behandlingType !in listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Klage)  ) {
            return Fullført
        }

        if (tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())) {
            return Fullført
        }

        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (avklaringsbehov.skalTilbakeføresEtterKvalitetssikring()) {
            return TilbakeføresFraKvalitetsikrer
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomKreverKvalitetssikring()) {
            return FantAvklaringsbehov(Definisjon.KVALITETSSIKRING)
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(repositoryProvider: RepositoryProvider): BehandlingSteg {
            return KvalitetssikringsSteg(repositoryProvider)
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }
    }
}
