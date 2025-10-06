package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_9Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_9Repository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime

class VurderBrudd11_9Løser(
    private val aktivitetsplikt11_9Repository: Aktivitetsplikt11_9Repository,
    private val sakRepository: SakRepository
) :
    AvklaringsbehovsLøser<VurderBrudd11_9Løsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        aktivitetsplikt11_9Repository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderBrudd11_9Løsning): LøsningsResultat {
        løsning.valider(kontekst)

        val iverksatteVurderinger =
            kontekst.kontekst.forrigeBehandlingId?.let { aktivitetsplikt11_9Repository.hentHvisEksisterer(it) }?.vurderinger
                ?: emptySet()
        val nyeVurderinger =
            løsning.aktivitetsplikt11_9Vurderinger
                .map { it.tilVurdering(kontekst.behandlingId(), kontekst.bruker, LocalDateTime.now()) }
                .toSet()

        val alleVurderinger = nyeVurderinger + iverksatteVurderinger

        aktivitetsplikt11_9Repository.lagre(kontekst.behandlingId(), alleVurderinger)

        return LøsningsResultat("Fullført")
    }


    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_BRUDD_11_9
    }

    private fun VurderBrudd11_9Løsning.valider(kontekst: AvklaringsbehovKontekst) {
        if (aktivitetsplikt11_9Vurderinger.isEmpty()) {
            throw UgyldigForespørselException("Må ha minst én vurdering")
        }
        val datoer = aktivitetsplikt11_9Vurderinger.map { it.dato }
        if (datoer.size != datoer.toSet().size) {
            throw UgyldigForespørselException("Kan ikke ha flere nye brudd på samme dato")
        }
        datoer.forEach {
            if (it.isAfter(LocalDate.now())) {
                throw UgyldigForespørselException("Brudd kan ikke være i fremtiden")
            }
            if (it.isBefore(sakRepository.hent(kontekst.kontekst.sakId).rettighetsperiode.fom)) {
                throw UgyldigForespørselException("Brudd kan ikke være før starten på rettighetsperioden")
            }
        }
    }
}
