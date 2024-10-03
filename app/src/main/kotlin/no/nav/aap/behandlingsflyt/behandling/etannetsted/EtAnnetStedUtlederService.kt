package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

class EtAnnetStedUtlederService(connection: DBConnection) {
    private val barnetilleggRepository = BarnetilleggRepository(connection)
    private val institusjonsoppholdRepository = InstitusjonsoppholdRepository(connection)

    fun harBehovForAvklaringer(behandlingId: BehandlingId): BehovForAvklaringer {
        val input = konstruerInput(behandlingId)

        return utledBehov(input)
    }

    private fun utledBehov(input: EtAnnetStedInput): BehovForAvklaringer {
        val opphold = input.institusjonsOpphold
        val soningsOppgold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.FO }
        val helseopphold = opphold.filter { segment -> segment.verdi.type == Institusjonstype.HS }
        val barnetillegg = input.barnetillegg

        if(soningsOppgold.isNotEmpty()){
            return BehovForAvklaringer(true, false)
        }

        val helseOppholdTidslinje = Tidslinje(helseopphold.map { segment ->
            Segment(
                segment.periode,
                true
            )
        })

        val barnetilleggTidslinje = Tidslinje(barnetillegg.map { segment ->
            Segment(
                segment.periode,
                true
            )
        })

        //fjern perioder hvor bruker har barnetillegg gjennom hele helseinstitusjonsoppholdet
        val oppholdUtenBarnetillegg = Tidslinje(helseOppholdTidslinje.filter { segment ->
            Tidslinje(segment.periode, false).kombiner(barnetilleggTidslinje,
                JoinStyle.LEFT_JOIN { periode, helseSegment, barnetilleggSegment ->
                    Segment(periode, barnetilleggSegment?.verdi == true)
                }
            ).map { seg -> seg.verdi.not() }.any()
        })

        // Oppholdet må være lengre enn 3 måneder for å være aktuelt for avklaring og må ha vart i minimum 2 måneder for å være klar for avklaring
        if (oppholdUtenBarnetillegg.segmenter().filter { segment -> segment.verdi }.any(
                { segment ->
                    val startTelling = LocalDate.of(segment.fom().year, segment.fom().month.plus(1).value,1)
                    startTelling.plusMonths(3) < segment.periode.tom && startTelling.plusMonths(2) >= LocalDate.now()
                }
            )
        ) {
            return BehovForAvklaringer(false, true)
        }

        return BehovForAvklaringer(false, false)
    }

    private fun konstruerInput(behandlingId: BehandlingId): EtAnnetStedInput {
        val grunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId)
        val barnetillegg = barnetilleggRepository.hentHvisEksisterer(behandlingId)?.perioder ?: emptyList()

        val opphold = grunnlag?.opphold ?: emptyList()

        return EtAnnetStedInput(opphold, barnetillegg)
    }
}