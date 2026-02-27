package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

enum class VedtakslengdeUtvidelse {
    /** Vedtakslengde skal utvides med ett helt år automatisk */
    AUTOMATISK,

    /** Vedtakslengde skal utvides under ett år (manuell behandling) */
    MANUELL,

    /** Ingen periode å utvide – utvidelse er ikke aktuell */
    IKKE_AKTUELL
}

