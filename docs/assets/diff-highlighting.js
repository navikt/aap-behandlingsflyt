// Dokka loads Prism asynchronously, so wait until all scripts have loaded.
window.addEventListener("load", () => {
    Prism.languages.diff = {
        coord: /^(?:diff .*|index .*|--- .*|\+\+\+ .*|@@.*)$/m,
        deleted: /^-.*/m,
        inserted: /^\+.*/m,
    };

    document.querySelectorAll("pre code.lang-diff, pre code.language-diff")
        .forEach((block) => Prism.highlightElement(block));
});
