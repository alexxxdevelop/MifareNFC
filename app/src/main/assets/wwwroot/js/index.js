let clickedCount = 0, clickedDate = new Date();
let set = {};
let videoIndex = 0, playVideoInterval;
let admin = false;
let timeoutId;

$(document).ready(function () {
    $('.tabs').tabs();
    $('.modal').append('<a class="close" onclick="closeDialog()"></a>');

    $('.modals').click(function () {
        closeDialog();
    });
    $('.modal').click(function (event) {
        event.stopPropagation();
    });
    $('#video').css('width', $('#video').width() + 'px');
    $('#algo').change(function (event) {
        $('.algo').hide();
        $('#algo' + $(this).val()).show();
    });
    $('#deviceType').change(function (event) {
        $('.deviceType0, .deviceType1').hide();
        $('.deviceType' + $(this).val()).show();
    });

    arrowAnim();
    currentTime();
    setInterval(currentTime, 1000);

    $('.accordion h3').click(function () {
        $(this).parent().find('.accordion-body').slideToggle(200);
        $(this).parent().toggleClass('acc-close').toggleClass('acc-open');
    });

    loadSettings();
    $('#border_color').minicolors();
    $('#arrow_color').minicolors();
});

function log(s) {
    $('#log').append('<div>{0}</div>'.format(s));
}

function passConfirm() {
    let pass = $('#pass1').val();
    if (pass == set.pass || pass == set.pass2) {
        admin = pass == set.pass2;
        $('#error_pass').hide();
        closeDialog();
        showHideSettings();
        showDialog('#admin');
    }
    else $('#error_pass').show();
}

function loadSettings() {
    try { set = JSON.parse(Android.loadSettings()); } catch {
        //set.media = 'https://disk.yandex.ru/d/jxQheBrAUnCBLw';
        set.text_center = 'ПРИЛОЖИТЕ БРАСЛЕТ ИЛИ КАРТУ К СЧИТЫВАТЕЛЮ';
        set.text_righttop = 'Инфотерминал';
        set.static_img_secs = 5;
        set.media_mins = 1;
        set.fields = [];
        set.fields.push({});
        set.fields1 = [];
        set.fields1.push({});
        set.fields1.push({});
        set.fields2 = [];
        set.fields2.push({});
        set.pass2 = '16052005';
        set.set8 = true;
        set.border_color = '#ab6e00';
        set.arrow_color = '#ab6e00';
        set.deviceType = 0;
        set.wiegandOrder = 0;
    }
    $('#pass2').val(set.pass);
    $('#pass2_service').val(set.pass2);
    $('#logo').text(set.logo);
    $('#media').val(set.media);
    $('#text_center').val(set.text_center);
    $('#text_righttop').val(set.text_righttop);
    $('#text_time').val(set.text_time);
    $('#static_img_secs').val(set.static_img_secs);
    $('#media_mins').val(set.media_mins);
    $('#border_color').val(set.border_color);
    $('#arrow_color').val(set.arrow_color);

    $('#cont_wait_gpio').val(set.cont_wait_gpio);
    $('#cont_text1').val(set.cont_text1);
    $('#cont_text2').val(set.cont_text2);
    $('#cont_text3').val(set.cont_text3);
    $('#cont_text4').val(set.cont_text4);
    $('#cont_text5').val(set.cont_text5);
    $('#cont_text_secs').val(set.cont_text_secs);
    $('#debugMode').prop('checked', set.debugMode);
    $('#wiegandOrder').val(set.wiegandOrder);
    $('#wiegandMode').val(set.wiegandMode);
    $('#portSpeed').val(set.portSpeed);
    $('#yandexToken').val(set.yandexToken);
    $('#yandexFolder').val(set.yandexFolder);
    $('#statusBar').prop('checked', set.statusBar);
    $('#navigationBar').prop('checked', set.navigationBar);

    if (set.fields) {
        set.fields.forEach((v) => {
            let field = $('#field_template').clone();
            field.removeAttr('id');
            field.find('.field-name').val(v.name);
            field.find('.field-name1').val(v.name1);
            field.find('.field-sector').text(v.sector);
            field.find('.field-block').text(v.block);
            field.find('.field-byte1').text(v.byte1);
            field.find('.field-byte2').text(v.byte2);
            field.find('.field-key').val(v.key);
            field.find('.field-conv').val(v.conv);
            field.appendTo('#fields').show();
        });
    }
    if (set.fields1) {
        set.fields1.forEach((v) => {
            let field = $('#field_template1').clone();
            field.removeAttr('id');
            field.find('.field-name').val(v.name);
            field.find('.field-name1').val(v.name1);
            field.find('.field-sector').text(v.sector);
            field.appendTo('#fields1').show();
        });
    }
    if (set.fields2) {
        set.fields2.forEach((v) => {
            let field = $('#field_template2').clone();
            field.removeAttr('id');
            field.find('.field-name').val(v.name);
            field.find('.field-name1').val(v.name1);
            field.find('.field-sector').text(v.sector);
            field.find('.field-key').val(v.key);
            field.appendTo('#fields2').show();
        });
    }
    if (set.algo == undefined) set.algo = 0;
    if (set.deviceType == undefined) set.deviceType = 0;
    $('#algo').val(set.algo); $('#algo').change();
    $('#deviceType').val(set.deviceType); $('#deviceType').change();

    applySettings();
}

function saveSettings() {
    admin = false;
    set.pass = $('#pass2').val();
    set.logo = $('#logo').text();
    set.media = $('#media').val();
    set.text_center = $('#text_center').val();
    set.text_righttop = $('#text_righttop').val();
    set.text_time = Number($('#text_time').val());
    set.static_img_secs = Number($('#static_img_secs').val());
    set.media_mins = Number($('#media_mins').val());
    set.border_color = $('#border_color').val();
    set.arrow_color = $('#arrow_color').val();

    set.cont_wait_gpio = Number($('#cont_wait_gpio').val());
    set.cont_text1 = $('#cont_text1').val();
    set.cont_text2 = $('#cont_text2').val();
    set.cont_text3 = $('#cont_text3').val();
    set.cont_text4 = $('#cont_text4').val();
    set.cont_text5 = $('#cont_text5').val();
    set.cont_text_secs = Number($('#cont_text_secs').val());
    set.debugMode = $('#debugMode').prop('checked');
    set.wiegandOrder = $('#wiegandOrder').val();
    set.wiegandMode = $('#wiegandMode').val();
    set.portSpeed = $('#portSpeed').val();
    set.yandexToken = $('#yandexToken').val();
    set.yandexFolder = $('#yandexFolder').val();
    set.statusBar = $('#statusBar').prop('checked');
    set.navigationBar = $('#navigationBar').prop('checked');

    set.fields = [];
    $('#fields .field').each(function() {
        let field = {};
        field.name = $(this).find('.field-name').val();
        field.name1 = $(this).find('.field-name1').val();
        field.sector = Number($(this).find('.field-sector').text());
        field.block = Number($(this).find('.field-block').text());
        field.byte1 = Number($(this).find('.field-byte1').text());
        field.byte2 = Number($(this).find('.field-byte2').text());
        field.key = $(this).find('.field-key').val();
        field.conv = Number($(this).find('.field-conv').val());
        set.fields.push(field);
    });
    set.fields1 = [];
    $('#fields1 .field').each(function () {
        let field = {};
        field.name = $(this).find('.field-name').val();
        field.name1 = $(this).find('.field-name1').val();
        field.sector = Number($(this).find('.field-sector').text());
        field.block = 0;
        field.byte1 = 0;
        field.byte2 = 0;
        field.key = '';
        field.conv = 0;
        set.fields1.push(field);
    });
    set.fields2 = [];
    $('#fields2 .field').each(function () {
        let field = {};
        field.name = $(this).find('.field-name').val();
        field.name1 = $(this).find('.field-name1').val();
        field.sector = Number($(this).find('.field-sector').text());
        field.block = 0;
        field.byte1 = 0;
        field.byte2 = 0;
        field.key = $(this).find('.field-key').val();
        field.conv = 0;
        set.fields2.push(field);
    });
    set.algo = $('#algo').val();
    set.deviceType = $('#deviceType').val();
    set.pass2 = $('#pass2_service').val();
    set.set1 = $('#set1').prop('checked');
    set.set2 = $('#set2').prop('checked');
    set.set3 = $('#set3').prop('checked');
    set.set4 = $('#set4').prop('checked');
    set.set5 = $('#set5').prop('checked');
    set.set6 = $('#set6').prop('checked');
    set.set7 = $('#set7').prop('checked');
    set.set8 = $('#set8').prop('checked');
    set.set9 = $('#set9').prop('checked');
    set.set10 = $('#set10').prop('checked');
    set.set11 = $('#set11').prop('checked');
    set.set12 = $('#set12').prop('checked');
    set.set13 = $('#set13').prop('checked');
    set.set14 = $('#set14').prop('checked');
    set.set15 = $('#set15').prop('checked');
    set.set16 = $('#set16').prop('checked');
    set.set20 = $('#set20').prop('checked');
    set.set21 = $('#set21').prop('checked');
    set.set22 = $('#set22').prop('checked');
    set.set23 = $('#set23').prop('checked');
    set.set24 = $('#set24').prop('checked');
    set.set25 = $('#set25').prop('checked');
    set.set26 = $('#set26').prop('checked');
    set.set27 = $('#set27').prop('checked');
    set.set29 = $('#set29').prop('checked');
    set.set30 = $('#set30').prop('checked');
    let json = JSON.stringify(set);
    try { Android.saveSettings(json); } catch { }
    closeDialog();
    applySettings();
}

function applySettings() {

    $('#logo_img').attr('src', set.logo);
    $('#text_center_div').text(set.text_center);
    $('#text_righttop_div').text(set.text_righttop);
    $('.border').css('border', '1px solid ' + set.border_color);
    $('.arrow path').attr('fill', set.arrow_color);
    if (set.debugMode) $('.debug').show(); else $('.debug').hide();

    handleNum();
    startPlayVideo();
}

function showHideSettings() {
    $('#set1').prop('checked', set.set1);
    $('#set2').prop('checked', set.set2);
    $('#set3').prop('checked', set.set3);
    $('#set4').prop('checked', set.set4);
    $('#set5').prop('checked', set.set5);
    $('#set6').prop('checked', set.set6);
    $('#set7').prop('checked', set.set7);
    $('#set8').prop('checked', set.set8);
    $('#set9').prop('checked', set.set9);
    $('#set10').prop('checked', set.set10);
    $('#set11').prop('checked', set.set11);
    $('#set12').prop('checked', set.set12);
    $('#set13').prop('checked', set.set13);
    $('#set14').prop('checked', set.set14);
    $('#set15').prop('checked', set.set15);
    $('#set16').prop('checked', set.set16);
    $('#set20').prop('checked', set.set20);
    $('#set21').prop('checked', set.set21);
    $('#set22').prop('checked', set.set22);
    $('#set23').prop('checked', set.set23);
    $('#set24').prop('checked', set.set24);
    $('#set25').prop('checked', set.set25);
    $('#set26').prop('checked', set.set26);
    $('#set27').prop('checked', set.set27);
    $('#set29').prop('checked', set.set29);
    $('#set30').prop('checked', set.set30);

    if (admin) $('#pass2_service_div').show(); else $('#pass2_service_div').hide();
    if (set.set1 || admin) $('#set1_div').show(); else $('#set1_div').hide();
    if (set.set2 || admin) $('#set2_div').show(); else $('#set2_div').hide();
    if (set.set3 || admin) $('#set3_div').show(); else $('#set3_div').hide();
    if (set.set4 || admin) $('#set4_div').show(); else $('#set4_div').hide();
    if (set.set5 || admin) $('#set5_div').show(); else $('#set5_div').hide();
    if (set.set6 || admin) $('#set6_div').show(); else $('#set6_div').hide();
    if (set.set7 || admin) $('#set7_div').show(); else $('#set7_div').hide();
    if (set.set8 || admin) $('#set8_div').show(); else $('#set8_div').hide();
    if (set.set9 || admin) $('#set9_div').show(); else $('#set9_div').hide();
    if (set.set10 || admin) $('#set10_div').show(); else $('#set10_div').hide();
    if (set.set11 || admin) $('#set11_div').show(); else $('#set11_div').hide();
    if (set.set12 || admin) $('#set12_div').show(); else $('#set12_div').hide();
    if (set.set13 || admin) $('#set13_div').show(); else $('#set13_div').hide();
    if (set.set14 || admin) $('#set14_div').show(); else $('#set14_div').hide();
    if (set.set15 || admin) $('#set15_div').show(); else $('#set15_div').hide();
    if (set.set16 || admin) $('#set16_div').show(); else $('#set16_div').hide();
    if (set.set20 || admin) $('#set20_div').show(); else $('#set20_div').hide();
    if (set.set21 || admin) $('#set21_div').show(); else $('#set21_div').hide();
    if (set.set22 || admin) $('#set22_div').show(); else $('#set22_div').hide();
    if (set.set23 || admin) $('#set23_div').show(); else $('#set23_div').hide();
    if (set.set24 || admin) $('#set24_div').show(); else $('#set24_div').hide();
    if (set.set25 || admin) $('#set25_div').show(); else $('#set25_div').hide();
    if (set.set26 || admin) $('#set26_div').show(); else $('#set26_div').hide();
    if (set.set27 || admin) $('#set27_div').show(); else $('#set27_div').hide();
    if (set.set29 || admin) $('#set29_div').show(); else $('#set29_div').hide();
    if (set.set30 || admin) $('#set30_div').show(); else $('#set30_div').hide();
    if (admin) $('#set28_div').show(); else $('#set28_div').hide();

    if (admin) $('#set1').show(); else $('#set1').hide();
    if (admin) $('#set2').show(); else $('#set2').hide();
    if (admin) $('#set3').show(); else $('#set3').hide();
    if (admin) $('#set4').show(); else $('#set4').hide();
    if (admin) $('#set5').show(); else $('#set5').hide();
    if (admin) $('#set6').show(); else $('#set6').hide();
    if (admin) $('#set7').show(); else $('#set7').hide();
    if (admin) $('#set8').show(); else $('#set8').hide();
    if (admin) $('#set9').show(); else $('#set9').hide();
    if (admin) $('#set10').show(); else $('#set10').hide();
    if (admin) $('#set11').show(); else $('#set11').hide();
    if (admin) $('#set12').show(); else $('#set12').hide();
    if (admin) $('#set13').show(); else $('#set13').hide();
    if (admin) $('#set14').show(); else $('#set14').hide();
    if (admin) $('#set15').show(); else $('#set15').hide();
    if (admin) $('#set16').show(); else $('#set16').hide();
    if (admin) $('#set20').show(); else $('#set20').hide();
    if (admin) $('#set21').show(); else $('#set21').hide();
    if (admin) $('#set22').show(); else $('#set22').hide();
    if (admin) $('#set23').show(); else $('#set23').hide();
    if (admin) $('#set24').show(); else $('#set24').hide();
    if (admin) $('#set25').show(); else $('#set25').hide();
    if (admin) $('#set26').show(); else $('#set26').hide();
    if (admin) $('#set27').show(); else $('#set27').hide();
    if (admin) $('#set29').show(); else $('#set29').hide();
    if (admin) $('#set30').show(); else $('#set30').hide();

    if (admin) $('#exitAndroid').show(); else $('#exitAndroid').hide();

    if (isPhone()) {
        $('#set3_div').hide();
        $('#set5_div').hide();
        $('#set7_div').hide();
        $('#set8_div').hide();
    }
}

function showLogo() {
    Android.showLogo();
    //$('#logo').text(s);
}

function showMedia(requestCode) {
    Android.showMedia(requestCode);
    //$('#media').text(s);
}

function arrowAnim() {
    /*$('#arrow').animate({ top: 20 }, 700, function () {
        $('#arrow').animate({ top: 0 }, 300, arrowAnim());
    });*/
}

function currentTime() {
    let d = new Date();
    $('.date').text(d.toLocaleDateString("ru-RU", { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' }));
    let h = d.getHours();
    let m = d.getMinutes();
    if (h < 10) h = '0' + h;
    if (m < 10) m = '0' + m;
    $('.time').text('{0}:{1}'.format(h, m));
}

function startPlayVideo() {
    clearInterval(playVideoInterval);
    playVideo();
    setInterval(playVideo, set.media_mins * 60000);
}

function playVideo() {
    videoIndex = 0;
    if (set.media) {
        if (set.media.indexOf('https://disk.yandex.ru/') == 0) {
            fetch('https://cloud-api.yandex.net/v1/disk/public/resources?public_key=' + set.media)
                .then(response => response.json())
                .then(j => {
                    let list = [];
                    j._embedded.items.forEach((item) => list.push(item.file));
                    autoPlay(list.join('|'));
                });
        }
        else {
            let s = Android.getMedia();
            autoPlay(s);
        }
    }
}

function autoPlay(s) {
    let videoSource = s.split('|');

    const videoCount = videoSource.length;
    if (videoSource[videoIndex].indexOf('.mp4') > -1) {
        if (!$('#video video').length) $('#video').append('<video style="height: 400px; display: none"></video>');
        const element = $('video')[0];

        function videoPlay(videoNum) {
            element.setAttribute("src", videoSource[videoNum]);
            element.autoplay = true;
            element.load();
            setTimeout(() => $(element).show(), 500);
        }
        element.addEventListener('ended', myHandler, false);

        videoPlay(videoIndex);
        ensureVideoPlays(); // play the video automatically

        function myHandler() {
            $('#video').html('');
            videoIndex++;
            if (videoIndex == videoCount) videoIndex = 0;
            autoPlay(s);
        }

        function ensureVideoPlays() {
            const video = $('video')[0];

            if (!video) return;

            const promise = video.play();
            if (promise !== undefined) {
                promise.then(() => {
                    // Autoplay started
                }).catch(error => {
                    // Autoplay was prevented.
                    video.muted = true;
                    video.play();
                });
            }
        }
    }
    else {
        $('#video').html('<img src="' + videoSource[videoIndex] + '" style="height: 400px" />');
        setTimeout(() => {
            $('#video').html('');
            videoIndex++;
            if (videoIndex == videoCount) videoIndex = 0;
            autoPlay(s);
        }, set.static_img_secs * 1000);
    }
}

function changeNumber(el) {
    let n = Number($(el).parent().text());
    if ($(el).hasClass('up')) n++; else n--;
    let min = $(el).parent().data('min');
    let max = $(el).parent().data('max');
    if (n < min) n = min;
    if (n > max) n = max;
    $(el).parent().find('span').text(n);
}

function addField() {
    $('#field_template').clone().removeAttr('id').appendTo('#fields').show();
    handleNum();
}

function addField1() {
    $('#field_template1').clone().removeAttr('id').appendTo('#fields1').show();
    handleNum();
}

function addField2() {
    $('#field_template2').clone().removeAttr('id').appendTo('#fields2').show();
    handleNum();
}

function exitAndroid() {
    Android.exitAndroid();
}

function showNfc(html, html_cont, secs) {
    showDialog('#nfc_dialog');
    html = html.replace('arrow_up.svg', $('#arrow_up').html());
    $('#nfc_html').html(html);
    if (html_cont != '') {
        $('#nfc_html_cont').show();
        $('#nfc_html_cont').html(html_cont);
    }
    else $('#nfc_html_cont').hide();
    $('.border').css('border', '1px solid ' + set.border_color);
    if (html == '') $('#nfc_html').hide(); else $('#nfc_html').show();
    clearTimeout(timeoutId);
    if (secs > 0) timeoutId = setTimeout(closeDialog, secs * 1000);
}

function handleNum() {
    $('.num').each(function () {
        if (!$(this).find('a').length) {
            let n = $(this).text();
            $(this).html('');
            $(this).append('<a class="down" onclick="changeNumber(this)"></a>');
            $(this).append('<span>' + n + '</span>');
            $(this).append('<a class="up" onclick="changeNumber(this)"></a>');
        }
    });
}

function showVar(name, value) {
    $('#' + name).text(value);
}

function saveSettingsToFile() {
    try { Android.saveSettingsToFile(); } catch { }
}

function loadSettingsFromFile() {
    try { Android.loadSettingsFromFile(); } catch { }
    loadSettings();
    log(set.pass)
}

//#region yandex
// Загрузка файла на Яндекс.Диск
async function uploadFile(items) {
    let d = new Date();
    // Создаем текстовый файл
    const blob = new Blob([items.replace(/\\"/g, '"').replace(/"\[/g, '[').replace(/\]"/g, ']').replace(/{n}/g, '\n')], { type: 'text/plain' });
    const file = new File([blob], 'file.txt');

    let path = `${$('#yandexFolder').val()}/${formatDateFile(d)}.csv`;
    let resp = await fetch(`https://cloud-api.yandex.net/v1/disk/resources/upload?path=${encodeURIComponent(path)}&overwrite=true`, { headers: { 'Authorization': `OAuth ${$('#yandexToken').val()}` } });
    let j = await resp.json();
    await fetch(j.href, { method: 'PUT', body: file });
}
//#endregion

//#region touch
document.addEventListener('touchstart', handleTouchStart, false);
document.addEventListener('touchmove', handleTouchMove, false);
let xDown = null, yDown = null;
let miny = window.innerHeight - window.innerHeight * 0.2;

function getTouches(evt) {
    return evt.touches ||             // browser API
        evt.originalEvent.touches; // jQuery
}

function handleTouchStart(evt) {
    const firstTouch = getTouches(evt)[0];
    xDown = firstTouch.clientX;
    yDown = firstTouch.clientY;
};

function handleTouchMove(evt) {
    if (!xDown || !yDown) {
        return;
    }

    var xUp = evt.touches[0].clientX;
    var yUp = evt.touches[0].clientY;

    var xDiff = xDown - xUp;
    var yDiff = yDown - yUp;

    if (Math.abs(xDiff) > Math.abs(yDiff)) {/*most significant*/
        if (xDiff > 0) {
            /* right swipe */
        } else {
            /* left swipe */
        }
    } else {
        if (yDiff > 0) {
            if (yDown > miny) {
                $("#buttons").animate({ bottom: 0 });
                setTimeout(() => $("#buttons").animate({ bottom: -100 }), 3000);
            }
        } else {
            $("#buttons").animate({ bottom: -100 });
        }
    }
    /* reset values */
    xDown = null;
    yDown = null;
};
//#endregion