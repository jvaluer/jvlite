<#assign pref = "">

<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes">

    <title>Задача ${name}</title>
    <meta name="description" content="jvlite-html description">

    <!-- See https://goo.gl/OOhYW5 -->
    <link rel="manifest" href="${pref}/manifest.json">
    <script src="${pref}/bower_components/webcomponentsjs/webcomponents-lite.js"></script>

    <link rel="import" href="${pref}/elems/jvlite-header.html">
    <link rel="import" href="${pref}/elems/jvlite-task.html">

    <link rel="stylesheet" href="/static/main.css">

</head>
<body>
<jvlite-header></jvlite-header>
<div class="content">
    <jvlite-task name="${name}"
                 subs='${subs}'
                 task-id="${id}"
     <#if nsignedIn>no-submit</#if>
    >
        ${statement}
    </jvlite-task>
</div>
</body>
</html>
