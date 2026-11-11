package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SykdomsvurderingForBrevLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider

class SykdomsvurderingForBrevLøser(
    private val sykdomsvurderingForBrevRepository: SykdomsvurderingForBrevRepository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<SykdomsvurderingForBrevLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sykdomsvurderingForBrevRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SykdomsvurderingForBrevLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())
        val kilder = løsning.kilder ?: sykdomsvurderingForBrevRepository.hent(behandling.id)?.kilder.orEmpty()
        if (kilder.any { it.journalpostId.isBlank() || it.dokumentInfoId.isBlank() || it.dokumentnavn.isBlank() }) {
            throw UgyldigForespørselException("Dokumentkilder må ha journalpost-ID, dokument-ID, dokumentnavn og journalføringsdato.")
        }
        if (kilder.distinctBy { it.journalpostId to it.dokumentInfoId }.size != kilder.size) {
            throw UgyldigForespørselException("Samme dokument kan ikke legges til som kilde flere ganger.")
        }
        val sykdomsvurderingForBrev = SykdomsvurderingForBrev(
            behandlingId = kontekst.behandlingId(),
            vurdering = løsning.vurdering,
            vurdertAv = kontekst.bruker,
            kilder = kilder,
        )
        sykdomsvurderingForBrevRepository.lagre(behandling.id, sykdomsvurderingForBrev)

        return LøsningsResultat("Skrevet sykdomsvurdering for brev")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_SYKDOMSVURDERING_BREV
    }
}
