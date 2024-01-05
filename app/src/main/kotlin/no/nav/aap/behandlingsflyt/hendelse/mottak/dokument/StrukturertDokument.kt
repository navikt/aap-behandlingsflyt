package no.nav.aap.behandlingsflyt.hendelse.mottak.dokument

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.behandling.dokumenter.Brevkode

class StrukturertDokument<T : PeriodisertData>(val data: T, val brevkode: Brevkode) {

    fun periode(): Periode {
        return data.periode()
    }
}