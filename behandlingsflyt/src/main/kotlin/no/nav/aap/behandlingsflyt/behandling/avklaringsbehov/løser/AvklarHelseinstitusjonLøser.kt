package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarHelseinstitusjonLøser(
    private val behandlingRepository: BehandlingRepository,
    private val helseinstitusjonRepository: InstitusjonsoppholdRepository,
) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        helseinstitusjonRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarHelseinstitusjonLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId())

        val vedtatteVurderinger =
            behandling.forrigeBehandlingId?.let { helseinstitusjonRepository.hentHvisEksisterer(it) }

        val oppdaterteVurderinger =
            slåSammenMedNyeVurderinger(
                vedtatteVurderinger,
                løsning.helseinstitusjonVurdering.vurderinger
            )
        helseinstitusjonRepository.lagreHelseVurdering(
            kontekst.kontekst.behandlingId,
            kontekst.bruker.ident,
            oppdaterteVurderinger
        )

        return LøsningsResultat(løsning.helseinstitusjonVurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    private fun slåSammenMedNyeVurderinger(
        grunnlag: InstitusjonsoppholdGrunnlag?,
        nyeVurderinger: List<HelseinstitusjonVurderingDto>,
    ): List<HelseinstitusjonVurdering> {
        val eksisterendeTidslinje = byggTidslinjeForHelseoppholdvurderinger(grunnlag)

        val nyeVurderingerTidslinje = Tidslinje(nyeVurderinger.sortedBy { it.periode }
            .map {
                Segment(
                    it.periode,
                    HelseoppholdVurderingData(
                        begrunnelse = it.begrunnelse,
                        faarFriKostOgLosji = it.faarFriKostOgLosji,
                        forsoergerEktefelle = it.forsoergerEktefelle,
                        harFasteUtgifter = it.harFasteUtgifter,
                    )
                )
            }).komprimer()

        return eksisterendeTidslinje.kombiner(
            nyeVurderingerTidslinje,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).segmenter().map {
            HelseinstitusjonVurdering(
                begrunnelse = it.verdi.begrunnelse,
                faarFriKostOgLosji = it.verdi.faarFriKostOgLosji,
                forsoergerEktefelle = it.verdi.forsoergerEktefelle,
                harFasteUtgifter = it.verdi.harFasteUtgifter,
                periode = it.periode
            )
        }
    }

    private fun byggTidslinjeForHelseoppholdvurderinger(grunnlag: InstitusjonsoppholdGrunnlag?): Tidslinje<HelseoppholdVurderingData> {
        if (grunnlag == null) {
            return Tidslinje()
        }
        return grunnlag.helseoppholdvurderinger?.tilTidslinje()
            ?.mapValue {
                HelseoppholdVurderingData(
                    begrunnelse = it.begrunnelse,
                    faarFriKostOgLosji = it.faarFriKostOgLosji,
                    forsoergerEktefelle = it.forsoergerEktefelle,
                    harFasteUtgifter = it.harFasteUtgifter
                )
            } ?: Tidslinje()
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }

    internal data class HelseoppholdVurderingData(
        val begrunnelse: String,
        val faarFriKostOgLosji: Boolean,
        val forsoergerEktefelle: Boolean? = null,
        val harFasteUtgifter: Boolean? = null,
    )
}