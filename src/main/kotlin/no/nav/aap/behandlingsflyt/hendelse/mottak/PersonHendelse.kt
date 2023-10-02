package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.domene.Periode

interface PersonHendelse {

    fun periode(): no.nav.aap.behandlingsflyt.domene.Periode

    fun tilSakshendelse(): SakHendelse
}
