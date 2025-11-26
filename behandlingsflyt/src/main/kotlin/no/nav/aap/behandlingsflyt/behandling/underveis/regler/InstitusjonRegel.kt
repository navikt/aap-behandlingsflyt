package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory

/**
 *  Utledning av hvilke perioder med innleggelse som kan gi reduksjon håndteres i et annet sted, alle opphold
 *  som kommer inn her har allerede vært vurdert for om de skal gi reduksjon eller ei.
 *
 *  Samt at de er avkortet ved å klippe bort innleggelsesmåneden.
 *
 *  Se EtAnnetStedUtlederService for logikken
 */
class InstitusjonRegel : UnderveisRegel {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        log.info("Vurderer institusjonsregel.")
        var institusjonTidslinje = konstruerTidslinje(input)
        if (institusjonTidslinje.isEmpty()) {
            return resultat
        }
        val reduksjonsTidslinje = utledTidslinjeHvorDetKanGisReduksjon(input)
        institusjonTidslinje = institusjonTidslinje.kombiner(reduksjonsTidslinje, sammenslåer())

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

    private fun sammenslåer(): JoinStyle<InstitusjonVurdering, Boolean, InstitusjonVurdering> {
        return JoinStyle.LEFT_JOIN { periode, venstreSegment, høyreSegment ->
            val venstreVerdi = venstreSegment.verdi
            val høyreVerdi = høyreSegment?.verdi

            val skalRedusere = venstreVerdi.skalReduseres && høyreVerdi == true
            var årsak: Årsak? = utledÅrsak(venstreVerdi.skalReduseres)
            var grad = Prosent.`100_PROSENT`
            if (skalRedusere) {
                årsak = requireNotNull(venstreVerdi.årsak)
                grad = Prosent.`50_PROSENT`
            }

            val verdi = InstitusjonVurdering(skalRedusere, årsak, grad)
            Segment(periode, verdi)
        }
    }

    private fun utledÅrsak(skalReduseres: Boolean?): Årsak? {
        if (skalReduseres == null) {
            return null
        }
        return if (skalReduseres) {
            Årsak.UTEN_REDUKSJON
        } else {
            null
        }
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

    private fun utledTidslinjeHvorDetKanGisReduksjon(input: UnderveisInput): Tidslinje<Boolean> {
        val tidslinjeOverInnleggelser = Tidslinje(
            input.institusjonsopphold.filter {
                it.institusjon?.erPåInstitusjon == true
            }.map {
                Segment(
                    it.periode,
                    it.institusjon?.skalGiUmiddelbarReduksjon == true
                )
            }
        ).komprimer()
        return Tidslinje(
            tidslinjeOverInnleggelser
                .segmenter()
                .map { Segment(utledMuligReduksjonsPeriode(it.periode, it.verdi), true) })
    }

    private fun utledMuligReduksjonsPeriode(periode: Periode, skalgiUmiddelbarReduksjon: Boolean): Periode {
        if (skalgiUmiddelbarReduksjon) {
            return Periode(periode.fom.withDayOfMonth(1).plusMonths(1), periode.tom)
        }
        return Periode(periode.fom.withDayOfMonth(1).plusMonths(1).plusMonths(3), periode.tom)
    }
}