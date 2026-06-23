function showDialog(el) {
    $(el).css('display', 'flex');
}
function closeDialog() {
    $('.modals').hide();
}

async function post(url, data, auth) {
    const formData = new FormData();
    for (const key in data) formData.append(key, data[key]);
    let init = {
        method: 'POST',
        body: formData
    };
    if (auth) {
        const encodedCredentials = btoa(auth);
        init.headers = { 'Authorization': `Basic ${encodedCredentials}` }
    }
    const response = await fetch(url, init);
    return response.json();
}

function formatDate(date) {
    const pad = (num) => num.toString().padStart(2, '0');

    const dd = pad(date.getDate());
    const mm = pad(date.getMonth() + 1); // мес€цы начинаютс€ с 0
    const yyyy = date.getFullYear();
    const hh = pad(date.getHours());
    const min = pad(date.getMinutes());
    const ss = pad(date.getSeconds());

    return `${dd}.${mm}.${yyyy} ${hh}:${min}:${ss}`;
}

function formatDateFile(date) {
    const pad = (num) => num.toString().padStart(2, '0');

    const dd = pad(date.getDate());
    const mm = pad(date.getMonth() + 1); // мес€цы начинаютс€ с 0
    const yyyy = date.getFullYear();
    const hh = pad(date.getHours());
    const min = pad(date.getMinutes());
    const ss = pad(date.getSeconds());

    return `${dd}${mm}${yyyy}_${hh}${min}${ss}`;
}

String.prototype.format = function () {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function (m, n) {
        return args[n];
    });
};

function isPhone() {
    const userAgent = navigator.userAgent.toLowerCase();
    const isAndroidPhone = /android/.test(userAgent) && /mobile/.test(userAgent);
    const isIPhone = /iphone/.test(userAgent);
    const isPhoneByScreenSize = window.matchMedia("(max-width: 787px)").matches;

    return isAndroidPhone || isIPhone || isPhoneByScreenSize;
}

(function ($) {

    $.fn.tabs = function (options) {

        return this.each(function () {
            var tabs = this;
            i = 0;

            var showPage = function (i) {
                $(tabs).children('div').hide();
                $(tabs).children('div').eq(i).show();
                $(tabs).children('ul').children('li').removeClass('active');
                $(tabs).children('ul').children('li').eq(i).addClass('active');
            }

            showPage(0);

            $(tabs).children('ul').children('li').each(function (index, element) {
                $(element).attr('data-page', i);
                i++;
            });

            $(tabs).children('ul').children('li').click(function () {
                showPage(parseInt($(this).attr('data-page')));
            });
        });
    };
})(jQuery);
function openTab(tab, selector = '.tabs') {
    $(selector + ' ul li[data-page="' + tab + '"]').click();
}