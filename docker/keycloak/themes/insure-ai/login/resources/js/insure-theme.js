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

  // The web app's origin: client_data on in-flow login URLs, redirect_uri on the first authorize URL.
  function appOrigin() {
    var params = new URLSearchParams(window.location.search);
    try {
      var data = params.get('client_data');
      if (data) {
        var json = JSON.parse(atob(data.replace(/-/g, '+').replace(/_/g, '/')));
        return new URL(json.ru).origin;
      }
      var redirect = params.get('redirect_uri');
      if (redirect) {
        return new URL(redirect).origin;
      }
    } catch (e) {
      /* fall through */
    }
    return null;
  }

  // Header and footer links are rendered by template.ftl; only their targets depend on the app origin.
  function build() {
    var origin = appOrigin();
    if (!origin) return;
    var links = document.querySelectorAll('[data-app-link]');
    for (var i = 0; i < links.length; i++) {
      links[i].setAttribute('href', origin + links[i].getAttribute('data-app-link'));
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', build);
  } else {
    build();
  }
})();
