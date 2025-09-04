package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_7Løsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate
import java.time.LocalDateTime

class VurderBrudd11_7Løser(
    private val aktivitetsplikt11_7Repository: Aktivitetsplikt11_7Repository,
    private val behandlingRepository: BehandlingRepository,
) : AvklaringsbehovsLøser<VurderBrudd11_7Løsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        aktivitetsplikt11_7Repository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderBrudd11_7Løsning): LøsningsResultat {
        val vurdering = løsning.aktivitetsplikt11_7Vurdering.tilVurdering(kontekst.bruker, LocalDateTime.now())

        val forrigeBehandlingId = behandlingRepository.hent(kontekst.kontekst.behandlingId).forrigeBehandlingId
        
        val gjeldendeVedtatte = forrigeBehandlingId
            ?.let { aktivitetsplikt11_7Repository.hentHvisEksisterer(it) }
            ?.tidslinje() ?: Tidslinje()

        val ny = Tidslinje(
            Periode(vurdering.gjelderFra, LocalDate.MAX), vurdering
        )

        val nyGjeldende = gjeldendeVedtatte.kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())

        aktivitetsplikt11_7Repository.lagre(
            kontekst.kontekst.behandlingId,
            vurderinger = nyGjeldende.toList().map { it.verdi }
        )

        return when (val validatedLøsning = løsning.valider()) {
            is Validation.Invalid -> throw UgyldigForespørselException(validatedLøsning.errorMessage)
            is Validation.Valid -> {
                LøsningsResultat(begrunnelse = løsning.aktivitetsplikt11_7Vurdering.begrunnelse)
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_BRUDD_11_7
    }

    private fun VurderBrudd11_7Løsning.valider(): Validation<VurderBrudd11_7Løsning> {
        if (aktivitetsplikt11_7Vurdering.erOppfylt xor (aktivitetsplikt11_7Vurdering.utfall == null)) {
            return Validation.Invalid(
                this,
                "Utfallet skal være satt hvis, og bare hvis, aktivitetsplikten ikke er oppfylt"
            )
        }
        return Validation.Valid(this)
    }
}