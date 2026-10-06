package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.utils.aggregerteFeilmeldinger
import no.nav.aap.behandlingsflyt.utils.tilNorskFormat
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBistandLøser(
    private val bistandRepository: BistandRepository,
) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        bistandRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {

        valider(løsning)

        val forrigeBehandlingId = kontekst.kontekst.forrigeBehandlingId

        val forrigeVedtatteGrunnlag = forrigeBehandlingId
            ?.let { bistandRepository.hentHvisEksisterer(it) }
        val vedtatteVurderinger = forrigeVedtatteGrunnlag?.vurderinger.orEmpty()

        val nyeVurderinger = løsning.løsningerForPerioder.map {
            it.tilBistandVurdering(
                kontekst.bruker,
                kontekst.behandlingId()
            )
        }

        bistandRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            bistandsvurderinger = vedtatteVurderinger + nyeVurderinger
        )
        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    private fun valider(løsning: AvklarBistandsbehovLøsning) {
        løsning.løsningerForPerioder
            .map { it.valider() }
            .aggregerteFeilmeldinger({ ugyldigVurdering ->
                "(Gjelder vurderinger fra: ${
                    ugyldigVurdering.joinToString(", ") { it.fom.tilNorskFormat() }
                })"
            })
            .takeIf { it.isNotEmpty() }
            ?.let { throw UgyldigForespørselException(it.joinToString("\n")) }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
