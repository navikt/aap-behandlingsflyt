package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn.ForeldreansvarVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBarnetilleggLøser(
    private val barnRepository: BarnRepository,
    private val sakRepository: SakRepository
) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        barnRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {
        val sak = sakRepository.hent(kontekst.kontekst.sakId)
        val flatBarn = løsning.vurderingerForBarnetillegg.vurderteBarn.flatMap { it.vurderinger }

        if (flatBarn.any {
            it.fraDato.isBefore(sak.rettighetsperiode.fom)
        }) {
            throw IllegalArgumentException("Kan ikke sette barnetilleggsperiode før rettighetsperioden.")
        }

        val vurderteBarn = barnRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId)?.vurderteBarn?.barn
            ?: emptyList()
        val oppdatertTilstand = oppdaterTilstandBasertPåNyeVurderinger(vurderteBarn, løsning)

        barnRepository.lagreVurderinger(kontekst.kontekst.behandlingId, oppdatertTilstand)

        return LøsningsResultat(begrunnelse = "Vurdert barnetillegg")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}

internal fun oppdaterTilstandBasertPåNyeVurderinger(
    vurderteBarn: List<VurdertBarn>,
    løsning: AvklarBarnetilleggLøsning
): List<VurdertBarn> {
    val tidslinjePerBarn = HashMap<String, Tidslinje<ForeldreansvarVurdering>>()
    vurderteBarn.forEach { barn -> tidslinjePerBarn[barn.ident.identifikator] = barn.tilTidslinje() }

    val nyeVurderinger = løsning.vurderingerForBarnetillegg.vurderteBarn

    nyeVurderinger.map { it.toVurdertBarn() }.forEach { nyVurdering ->
        val eksisterendeTidslinje = tidslinjePerBarn[nyVurdering.ident.identifikator] ?: Tidslinje()
        val oppdatertTidslinje = eksisterendeTidslinje.kombiner(
            nyVurdering.tilTidslinje(),
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        tidslinjePerBarn[nyVurdering.ident.identifikator] = oppdatertTidslinje.komprimer()
    }

    val oppdatertTilstand = tidslinjePerBarn.map {
        VurdertBarn(
            ident = Ident(it.key),
            vurderinger = it.value.segmenter().map {
                VurderingAvForeldreAnsvar(
                    it.periode.fom,
                    it.verdi.harForeldreAnsvar,
                    it.verdi.begrunnelse
                )
            })
    }
    return oppdatertTilstand
}
