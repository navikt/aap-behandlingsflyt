package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.steg.krav.KravRepository
import no.nav.aap.behandlingsflyt.steg.krav.KravValidering
import no.nav.aap.behandlingsflyt.steg.krav.KravValidering.validerRelevantKrav
import no.nav.aap.krav.KravVurdering
import no.nav.aap.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class VurderKravLøser(
    private val kravRepository: KravRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
) :
    AvklaringsbehovsLøser<VurderKravLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        kravRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderKravLøsning): LøsningsResultat {
        val søknaderISak =
            mottattDokumentRepository.hentDokumenterAvType(kontekst.sakId(), InnsendingType.SØKNAD)

        val nyeVurderinger = løsning.kravVurderinger.map { vurderingDto ->
            vurderingDto.tilVurdering(kontekst.behandlingId(), kontekst.bruker, Instant.now())
        }.toSet()

        validerGyldighet(kontekst.behandlingId(), nyeVurderinger, søknaderISak)

        val iverksatteVurderinger = kontekst.kontekst.forrigeBehandlingId?.let {
            kravRepository.hentHvisEksisterer(it)
        }?.vurderinger.orEmpty()

        val alleVurderinger = nyeVurderinger + iverksatteVurderinger

        kravRepository.lagre(kontekst.behandlingId(), alleVurderinger)

        return LøsningsResultat("Fullført")
    }

    private fun validerGyldighet(
        behnadlingId: BehandlingId,
        vurderinger: Set<KravVurdering>,
        søknaderISak: Set<MottattDokument>
    ) {
        val søknaderIBehandling = søknaderISak.filter { it.behandlingId == behnadlingId }.toSet()

        if (!KravValidering.erKravVurderingTilstrekkeligVurdert(søknaderIBehandling, vurderinger)) {
            throw UgyldigForespørselException("Mangler vurdering av krav for innsendt søknad.")
        }

        vurderinger.forEach { vurdering ->
            if (vurdering is RelevantKrav) {
                validerRelevantKrav(
                    vurdering,
                    søknaderISak.find { it.referanse.asJournalpostId == vurdering.journalpostId })
            }

        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KRAV
    }

}
