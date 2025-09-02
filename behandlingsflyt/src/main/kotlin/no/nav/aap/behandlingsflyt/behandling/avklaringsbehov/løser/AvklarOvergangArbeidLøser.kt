package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangArbeidLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class AvklarOvergangArbeidLøser(
    private val behandlingRepository: BehandlingRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarOvergangArbeidLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
    )


    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarOvergangArbeidLøsning
    ): LøsningsResultat {

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyesteSykdomsvurdering = sykdomRepository.hentHvisEksisterer(behandling.id)
            ?.sykdomsvurderinger?.maxByOrNull { it.opprettet }

        val overgangArbeidVurdering = løsning.overgangArbeidVurdering.tilOvergangArbeidVurdering(
            kontekst.bruker,
            nyesteSykdomsvurdering?.vurderingenGjelderFra
        )

        val eksisterendeOvergangarbeidvurderinger = behandling.forrigeBehandlingId
            ?.let { overgangArbeidRepository.hentHvisEksisterer(it) }
            ?.somOvergangArbeidvurderingstidslinje(LocalDate.MIN)
            ?: Tidslinje()

        val ny = overgangArbeidVurdering.let {
            OvergangArbeidGrunnlag(
                id = null,
                vurderinger = listOf(it),
            ).somOvergangArbeidvurderingstidslinje(LocalDate.MIN)
        }

        val gjeldende = eksisterendeOvergangarbeidvurderinger
            .kombiner(ny, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            .toList().map { it.verdi }

        overgangArbeidRepository.lagre(
            behandlingId = behandling.id,
            overgangArbeidVurderinger = gjeldende
        )

        return LøsningsResultat(
            begrunnelse = overgangArbeidVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_OVERGANG_ARBEID
    }
}