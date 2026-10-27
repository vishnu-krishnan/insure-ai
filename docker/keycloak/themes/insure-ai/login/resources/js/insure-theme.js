// Follow the colour scheme chosen in the web app (it publishes it in a cookie shared by host);
// without the cookie the page keeps following the system setting.
(function () {
  function chosen() {
    var match = document.cookie.match(/(?:^|;\s*)insure_theme=(light|dark)/);
    return match ? match[1] : null;
  }
  function apply() {
    var scheme = chosen();
    if (scheme) {
      document.documentElement.classList.toggle('pf-v5-theme-dark', scheme === 'dark');
    }
  }
  apply();
  document.addEventListener('DOMContentLoaded', apply);
  window.addEventListener('load', apply);
})();
