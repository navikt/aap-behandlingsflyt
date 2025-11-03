package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSoningsforholdLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.SoningsvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSoningsforholdLøser(
    private val behandlingRepository: BehandlingRepository,
    private val soningRepository: InstitusjonsoppholdRepository,
) : AvklaringsbehovsLøser<AvklarSoningsforholdLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        soningRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSoningsforholdLøsning
    ): LøsningsResultat {

        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { soningRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(vedtatteVurderinger, løsning.soningsvurdering.vurderinger)

        soningRepository.lagreSoningsVurdering(
            kontekst.kontekst.behandlingId,
            kontekst.bruker.ident,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.soningsvurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<SoningsvurderingDto>
    ): List<Soningsvurdering> {
        val eksisterendeTidslinje = byggTidslinjeForSoningsvurderinger(grunnlag)

        val nyeVurderingerTidslinje = nyeVurderinger.sortedBy { it.fraDato }
            .map {
                Tidslinje(
                    Periode(it.fraDato, Tid.MAKS),
                    SoningsvurderingData(it.skalOpphore, it.begrunnelse)
                )
            }
            .fold(Tidslinje<SoningsvurderingData>()) { acc, tidslinje ->
                acc.kombiner(tidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }.komprimer()

        return eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).segmenter()
            .map { Soningsvurdering(it.verdi.skalOpphøre, it.verdi.begrunnelse, it.periode.fom) }
    }

    private fun byggTidslinjeForSoningsvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?): Tidslinje<SoningsvurderingData> {
        if (grunnlag == null) {
            return Tidslinje()
        }
        return grunnlag.soningsVurderinger?.tilTidslinje()
            ?.mapValue { SoningsvurderingData(it.skalOpphøre, it.begrunnelse) }
            .orEmpty()
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SONINGSFORRHOLD
    }

    internal data class SoningsvurderingData(
        val skalOpphøre: Boolean,
        val begrunnelse: String,
    )
}
