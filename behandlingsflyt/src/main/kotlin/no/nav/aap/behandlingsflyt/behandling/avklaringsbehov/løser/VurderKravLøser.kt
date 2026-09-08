package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravValidering.validerRelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant

class VurderKravLøser(
    private val kravRepository: KravRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val sakService: SakService,
) :
    AvklaringsbehovsLøser<VurderKravLøsning> {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        kravRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider, gatewayProvider)
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

        // Midlertidig hack for å overstyre startdato. På sikt burde kravet i seg selv være nok til å sette datoen
        nyeVurderinger.filterIsInstance<MigrertKrav>().singleOrNull()?.let {
            sakService.overstyrRettighetsperioden(
                sakId = kontekst.sakId(),
                startDato = it.muligRettFra,
                sluttDato = Tid.MAKS
            )
        }

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

        val migrerteKravVurderinger = vurderinger.filterIsInstance<MigrertKrav>()

        if (migrerteKravVurderinger.size > 1) {
            throw UgyldigForespørselException("Man kan kun sende inn 1 migrert krav.")
        }

        vurderinger.forEach { vurdering ->
            if (vurdering is RelevantKrav) {
                validerRelevantKrav(
                    vurdering,
                    søknaderISak.find { it.referanse.asJournalpostId == vurdering.journalpostId })
            }

            if (vurdering is MigrertKrav) {
                KravValidering.validerMigrertKrav(vurdering)
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KRAV
    }

}
