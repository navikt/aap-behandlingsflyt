package no.nav.aap.behandlingsflyt.behandling.underveis.regler
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent

// §11-26
class SoningRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val soningTidslinje = konstruerTidslinje(input)
        if (soningTidslinje.segmenter().size < 1) {
            return resultat
        }

        return resultat.kombiner(soningTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                var venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi = venstreVerdi.leggTilInstitusjonVurdering(høyreSegment.verdi)
                    val originalGradering = requireNotNull(venstreVerdi.gradering())

                    if (høyreSegment.verdi.skalReduseres) {
                        venstreVerdi = venstreVerdi.leggTilGradering(
                            Gradering(
                                originalGradering.totaltAntallTimer,
                                originalGradering.andelArbeid,
                                Prosent.`0_PROSENT`
                            )
                        )
                    }
                }
                Segment(periode, venstreVerdi)
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<InstitusjonVurdering> {
        return Tidslinje(
            input.etAnnetSted.filter {
                it.soning.soner
            }.map {
                if (it.soning.formueUnderForvaltning) {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = true, it.begrunnelse, Årsak.FORMUE_UNDER_FORVALTNING, Prosent.`0_PROSENT`))
                }
                else if (!it.soning.soningUtenforFengsel && it.soning.arbeidUtenforAnstalt) {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.begrunnelse, Årsak.ARBEID_UTENFOR_ANSTALT, Prosent.`100_PROSENT`))
                }
                else if(!it.soning.soningUtenforFengsel)  {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = true, it.begrunnelse, Årsak.SONER_I_FENGSEL, Prosent.`0_PROSENT`))
                }
                else {
                    Segment(it.periode, InstitusjonVurdering(skalReduseres = false, it.begrunnelse, Årsak.SONER_UTENFOR_FENGSEL, Prosent.`100_PROSENT`))
                }
            }
        )
    }
}