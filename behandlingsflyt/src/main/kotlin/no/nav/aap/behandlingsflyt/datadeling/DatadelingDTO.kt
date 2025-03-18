package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class SakStatusDTO(
    val ident: String,
    val status: SakStatus
)

data class SakStatus(
    val sakId: String,
    val statusKode: VedtakStatus,
    val periode: Periode,
    val kilde: Kilde = Kilde.KELVIN
) {
    companion object {
        fun fromKelvin(saksnummer: String, status: Status, periode: Periode): SakStatus {
            return SakStatus(
                sakId = saksnummer,
                statusKode = VedtakStatus.valueOf(status.toString()),
                periode = periode,
            )
        }
    }

    enum class Kilde {
        ARENA,
        KELVIN
    }

    enum class VedtakStatus {
        AVSLU,
        FORDE,
        GODKJ,
        INNST,
        IVERK,
        KONT,
        MOTAT,
        OPPRE,
        REGIS,
        UKJENT,
        OPPRETTET,
        UTREDES,
        LØPENDE,
        AVSLUTTET

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SakStatus

        if (sakId != other.sakId) return false
        if (statusKode != other.statusKode) return false
        if (periode != other.periode) return false
        if (kilde != other.kilde) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sakId.hashCode()
        result = 31 * result + statusKode.hashCode()
        result = 31 * result + periode.hashCode()
        result = 31 * result + kilde.hashCode()
        return result
    }
}
