var map;
var rawPolyline, fragPolyline;

var activeFragment = 0;

function refreshTrack() {
    if (track.analyzed.length == 0) {
        $("#status").text("Пустой трек");
        $("#prev, #next").attr("disabled", "1").removeClass("btn-primary").addClass("btn-default");
        return;
    }

    $("#status").text("Номер текущего фрагмента: " + (activeFragment + 1));
    $("#fragmentTable").find("tr").removeClass("success").removeClass("text-success");
    $("#fragmentTable").find("tr[data-id="+ activeFragment +"]").addClass("success").addClass("text-success");

    var frag = track.analyzed[activeFragment];

    if (fragPolyline != null)
        fragPolyline.setMap(null);

    fragPolyline = new google.maps.Polyline({
        path: track.raw.slice(frag.startPoint, frag.endPoint + 1),
        geodesic: true,
        strokeColor: '#ff5500',
        strokeOpacity: 0.5,
        strokeWeight: 2
    });

    fragPolyline.setMap(map);
}

function initMap() {
    var avgLat = 0, avgLng = 0;

    $.each(track.raw, function(t) {
        avgLat += this.lat;
        avgLng += this.lng;
    });

    avgLat /= track.raw.length;
    avgLng /= track.raw.length;

    if (track.raw.length == 0) {
        avgLat = 55.72711009;
        avgLng = 37.53839493;
    }

    map = new google.maps.Map(document.getElementById('mapArea'), {
        center: { lat: avgLat, lng: avgLng },
        zoom: 12
    });

    $("#prev").click(function () {
        activeFragment = activeFragment - 1;
        if (activeFragment == -1)
            activeFragment = track.analyzed.length - 1;
        refreshTrack();
    });

    $("#next").click(function () {
        activeFragment = (activeFragment + 1) % track.analyzed.length;
        refreshTrack();
    });

    refreshTrack();
}