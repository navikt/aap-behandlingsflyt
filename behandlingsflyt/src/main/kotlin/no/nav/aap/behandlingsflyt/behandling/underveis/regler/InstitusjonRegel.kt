package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory

/**
 *  Utledning av hvilke perioder med innleggelse som kan gi reduksjon håndteres i institusjonsopphold, alle opphold
 *  som kommer inn her har allerede vært vurdert for om de skal gi reduksjon eller ei.
 *
 *  Samt at de er avkortet ved å klippe bort innleggelsesmåneden.
 *
 *  Se InstitusjonsoppholdUtlederService for logikken
 */
class InstitusjonRegel : UnderveisRegel {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        log.info("Vurderer institusjonsregel.")
        var institusjonTidslinje = konstruerTidslinje(input)
        if (institusjonTidslinje.isEmpty()) {
            return resultat
        }

        return resultat.kombiner(institusjonTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
                var venstreVerdi = venstreSegment.verdi
                if (høyreSegment?.verdi != null) {
                    venstreVerdi = venstreVerdi.leggTilInstitusjonVurdering(høyreSegment.verdi)
                }
                Segment(periode, venstreVerdi)
            }
        )
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<InstitusjonVurdering> {
        return Tidslinje(
            input.institusjonsopphold.filter {
                it.institusjon?.erPåInstitusjon == true
            }.map {
                if (it.institusjon?.skalGiReduksjon == false) {
                    Segment(
                        it.periode,
                        InstitusjonVurdering(
                            skalReduseres = false,
                            Årsak.FORSØRGER_ELLER_HAR_FASTEKOSTNADER,
                            Prosent.`100_PROSENT`
                        )
                    )
                } else {
                    Segment(
                        it.periode,
                        InstitusjonVurdering(skalReduseres = true, Årsak.KOST_OG_LOSJI, Prosent.`50_PROSENT`)
                    )
                }
            }
        ).komprimer()
    }
}