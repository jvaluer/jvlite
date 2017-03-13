<#assign pref = "">

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes">

    <title>Статус системы</title>
    <meta name="description" content="jvlite-html description">

    <!-- See https://goo.gl/OOhYW5 -->
    <link rel="manifest" href="${pref}/manifest.json">
    <script src="${pref}/bower_components/webcomponentsjs/webcomponents-lite.js"></script>

    <link rel="import" href="${pref}/elems/jvlite-header.html">
    <link rel="import" href="${pref}/elems/jvlite-status.html">

    <style>
        html, body {
            margin: 0;
            font-family: 'Roboto', 'Noto', sans-serif;
            -webkit-font-smoothing: antialiased;
            background: #f1f1f1;
        }
    </style>
</head>
<body>
<jvlite-header></jvlite-header>
<div class="content">
    <jvlite-status subs='${subs}'>
    </jvlite-status>
</div>
</body>
</html>