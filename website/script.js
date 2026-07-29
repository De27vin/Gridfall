document.getElementById("year").textContent = new Date().getFullYear();

const releaseVersion = document.getElementById("release-version");
const releaseDate = document.getElementById("release-date");
const releaseNotesList = document.getElementById("release-notes-list");
const releaseHistoryList = document.getElementById("release-history-list");

if (releaseVersion && releaseDate && releaseNotesList) {
  fetch("release.json")
    .then((response) => {
      if (!response.ok) throw new Error("Release metadata is unavailable");
      return response.json();
    })
    .then((release) => {
      releaseVersion.textContent = `Version: ${release.version}`;
      releaseDate.textContent = release.releasedAt ? `Updated: ${release.releasedAt}` : "Current Gridfall build.";
      releaseNotesList.replaceChildren(
        ...(release.highlights || []).map((highlight) => {
          const item = document.createElement("li");
          item.textContent = highlight;
          return item;
        })
      );
      if (releaseHistoryList && release.history) {
        releaseHistoryList.replaceChildren(
          ...release.history.map((entry) => {
            const item = document.createElement("li");
            const version = document.createElement("strong");
            version.textContent = `v${entry.version}`;
            item.append(version, ` (${entry.releasedAt}) ${entry.summary}`);
            return item;
          })
        );
      }
    })
    .catch(() => {
      // The static HTML values keep direct file previews usable.
    });
}
