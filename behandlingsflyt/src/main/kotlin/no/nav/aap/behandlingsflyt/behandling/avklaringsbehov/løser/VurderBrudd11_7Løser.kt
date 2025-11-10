package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime

class VurderBrudd11_7Løser(
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) : AvklaringsbehovsLøser<VurderBrudd11_7Løsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        aktivitetsplikt11_7Repository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderBrudd11_7Løsning): LøsningsResultat {
        løsning.valider()

        val vurdering = løsning.aktivitetsplikt11_7Vurdering.tilVurdering(
            vurdertIBehandling = kontekst.behandlingId(),
            bruker = kontekst.bruker,
            dato = LocalDateTime.now()
        )

        val forrigeBehandlingId = behandlingRepository.hent(kontekst.kontekst.behandlingId).forrigeBehandlingId

        val gjeldendeVedtatte =
            forrigeBehandlingId?.let { aktivitetsplikt11_7Repository.hentHvisEksisterer(it) }
                ?.tidslinje()
                .orEmpty()

        val ny = Tidslinje(
            Periode(vurdering.gjelderFra, Tid.MAKS), vurdering
        )

        val nyGjeldende = gjeldendeVedtatte.kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .komprimer()

        aktivitetsplikt11_7Repository.lagre(
            kontekst.kontekst.behandlingId, vurderinger = nyGjeldende.segmenter().map { it.verdi })

        settPåVentHvisInnenforFristenOgBrevAlleredeSendt(kontekst.behandlingId(), vurdering)

        return LøsningsResultat(begrunnelse = løsning.aktivitetsplikt11_7Vurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_BRUDD_11_7
    }

    private fun settPåVentHvisInnenforFristenOgBrevAlleredeSendt(
        behandlingId: BehandlingId,
        vurdering: Aktivitetsplikt11_7Vurdering
    ) {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val ventebehov = avklaringsbehov.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)

        if (ventebehov != null && !ventebehov.fristUtløpt() && !vurdering.erOppfylt && !vurdering.skalIgnorereVarselFrist) {
            avklaringsbehov.reåpne(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT)
        }
    }

    private fun VurderBrudd11_7Løsning.valider() {
        if (aktivitetsplikt11_7Vurdering.erOppfylt xor (aktivitetsplikt11_7Vurdering.utfall == null)) {
            throw UgyldigForespørselException(
                "Utfallet skal være satt hvis, og bare hvis, aktivitetsplikten ikke er oppfylt"
            )
        }
    }
}