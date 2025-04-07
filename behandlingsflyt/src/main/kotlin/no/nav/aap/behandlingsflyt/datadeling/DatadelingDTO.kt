package no.nav.aap.behandlingsflyt.datadeling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.type.Periode

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
}
