package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarStønadsperiodeLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.IkkeTilstrekkeligVurdert
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeValidering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class AvklarStønadsperiodeLøser(
    private val kravRepository: KravRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository
) :
    AvklaringsbehovsLøser<AvklarStønadsperiodeLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarStønadsperiodeLøsning): LøsningsResultat {
        val nyeVurderinger = løsning.stønadsperiodeVurderinger.map { vurderingDto ->
            vurderingDto.tilVurdering(kontekst.bruker, kontekst.behandlingId(), Instant.now())
        }.toSet()

        val iverksatteVurderinger = kontekst.kontekst.forrigeBehandlingId?.let {
            stønadsperiodeRepository.hentHvisEksisterer(it)
        }?.vurderinger.orEmpty()

        val alleVurderinger = nyeVurderinger + iverksatteVurderinger

        val relevanteKrav =
            kravRepository.hentHvisEksisterer(kontekst.behandlingId())?.gjeldendeRelevanteKrav().orEmpty()

        StønadsperiodeValidering.evaluerTilstrekkeligVurdert(relevanteKrav, alleVurderinger).let {
            if (it is IkkeTilstrekkeligVurdert) {
                throw UgyldigForespørselException(it.melding)
            }
        }
        
        stønadsperiodeRepository.lagre(kontekst.behandlingId(), alleVurderinger)

        return LøsningsResultat("Fullført")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_STØNADSPERIODE
    }

}
