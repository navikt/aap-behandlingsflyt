package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn.ForeldreansvarVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBarnetilleggLøser(
    private val barnRepository: BarnRepository,
    private val personRepository: PersonRepository,
) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        barnRepository = repositoryProvider.provide(), personRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {
        val alleredeLagredeVurderteBarn =
            barnRepository.hentVurderteBarnHvisEksisterer(kontekst.kontekst.behandlingId)?.barn.orEmpty()

        val løsningVurderteBarn = løsning.vurderingerForBarnetillegg.vurderteBarn.map {
            val identifikator = if (it.ident != null) {
                val person = personRepository.finn(Ident(it.ident))
                if (person != null) {
                    BarnIdentifikator.RegistertBarnPerson(person.id)
                } else {
                    BarnIdentifikator.BarnIdent(Ident(it.ident))
                }
            } else {
                BarnIdentifikator.NavnOgFødselsdato(it.navn!!, Fødselsdato(it.fødselsdato!!))
            }

            VurdertBarn(
                ident = identifikator,
                vurderinger = it.vurderinger.map { it.tilVurderingAvForeldreAnsvar() }
            )
        }

        val oppdatertTilstand =
            oppdaterTilstandBasertPåNyeVurderinger(
                alleredeLagredeVurderteBarn,
                løsningVurderteBarn
            )

        barnRepository.lagreVurderinger(kontekst.kontekst.behandlingId, kontekst.bruker.ident, oppdatertTilstand)

        return LøsningsResultat(begrunnelse = "Vurdert barnetillegg")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}

internal fun oppdaterTilstandBasertPåNyeVurderinger(
    vurderteBarn: List<VurdertBarn>,
    nyeVurderinger: List<VurdertBarn>
): List<VurdertBarn> {
    val tidslinjePerBarn = HashMap<BarnIdentifikator, Tidslinje<ForeldreansvarVurdering>>()
    vurderteBarn.forEach { barn -> tidslinjePerBarn[barn.ident] = barn.tilTidslinje() }

    nyeVurderinger.forEach { nyVurdering ->
        val eksisterendeTidslinje = tidslinjePerBarn[nyVurdering.ident] ?: Tidslinje()
        val oppdatertTidslinje = eksisterendeTidslinje.kombiner(
            nyVurdering.tilTidslinje(),
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        tidslinjePerBarn[nyVurdering.ident] = oppdatertTidslinje.komprimer()
    }

    val oppdatertTilstand = tidslinjePerBarn.map { (key, value) ->
        VurdertBarn(
            ident = key,
            vurderinger = value.segmenter().map {
                VurderingAvForeldreAnsvar(
                    it.periode.fom,
                    it.verdi.harForeldreAnsvar,
                    it.verdi.begrunnelse
                )
            })
    }
    return oppdatertTilstand
}
