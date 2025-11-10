package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.SaksbehandlerOppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn.ForeldreansvarVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBarnetilleggLøser(
    private val barnRepository: BarnRepository,
) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        barnRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {
        lagreSaksbehandlerOppgitteBarnHvisNye(kontekst, løsning)

        val oppdatertTilstand =
            oppdaterTilstandBasertPåNyeVurderinger(emptyList(), løsning.vurderingerForBarnetillegg.vurderteBarn)

        barnRepository.lagreVurderinger(kontekst.kontekst.behandlingId, kontekst.bruker.ident, oppdatertTilstand)

        return LøsningsResultat(begrunnelse = "Vurdert barnetillegg")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }

    private fun lagreSaksbehandlerOppgitteBarnHvisNye(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBarnetilleggLøsning
    ) {
        val eksisterendeBarn = barnRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId)
            ?.saksbehandlerOppgitteBarn?.barn.orEmpty()

        val nyeBarnListe = løsning.vurderingerForBarnetillegg.saksbehandlerOppgitteBarn
            .map { it.tilSaksbehandlerOppgittBarn() }

        // Sjekk om det er endringer (nye barn lagt til eller eksisterende barn fjernet)
        val erEndringer = eksisterendeBarn.size != nyeBarnListe.size ||
                nyeBarnListe.any { nyttBarn -> eksisterendeBarn.none { it.erSammeBarnSom(nyttBarn) } }

        if (erEndringer) {
            // Lagre nye barn og deaktiver de som ikke lenger er i listen
            barnRepository.lagreSaksbehandlerOppgitteBarn(kontekst.kontekst.behandlingId, nyeBarnListe)
        }
    }
}

private fun VurdertBarnDto.tilSaksbehandlerOppgittBarn() = SaksbehandlerOppgitteBarn.Barn(
    ident = ident?.let { Ident(it) },
    navn = requireNotNull(navn) { "Navn må være satt for saksbehandler oppgitt barn" },
    fødselsdato = Fødselsdato(requireNotNull(fødselsdato) { "Fødselsdato må være satt" }),
    relasjon = requireNotNull(oppgittForeldreRelasjon) { "Foreldrerelasjon må være satt" }
)

private fun SaksbehandlerOppgitteBarn.Barn.erSammeBarnSom(annet: SaksbehandlerOppgitteBarn.Barn): Boolean =
    if (ident != null && annet.ident != null) {
        ident == annet.ident
    } else {
        navn.equals(annet.navn, ignoreCase = true) && fødselsdato == annet.fødselsdato
    }

internal fun oppdaterTilstandBasertPåNyeVurderinger(
    vurderteBarn: List<VurdertBarn>,
    nyeVurderinger: List<VurdertBarnDto>
): List<VurdertBarn> {
    val tidslinjePerBarn = HashMap<BarnIdentifikator, Tidslinje<ForeldreansvarVurdering>>()
    vurderteBarn.forEach { barn -> tidslinjePerBarn[barn.ident] = barn.tilTidslinje() }

    nyeVurderinger.map { it.toVurdertBarn() }.forEach { nyVurdering ->
        val eksisterendeTidslinje = tidslinjePerBarn[nyVurdering.ident].orEmpty()
        val oppdatertTidslinje = eksisterendeTidslinje.kombiner(
            nyVurdering.tilTidslinje(),
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        tidslinjePerBarn[nyVurdering.ident] = oppdatertTidslinje.komprimer()
    }

    val oppdatertTilstand = tidslinjePerBarn.map { (identifikator, tidslinje) ->
        VurdertBarn(
            ident = identifikator,
            vurderinger = tidslinje.segmenter().map {
                VurderingAvForeldreAnsvar(
                    fraDato = it.periode.fom,
                    harForeldreAnsvar = it.verdi.harForeldreAnsvar,
                    erFosterForelder = it.verdi.erFosterforelder,
                    begrunnelse = it.verdi.begrunnelse
                )
            })
    }
    return oppdatertTilstand
}
