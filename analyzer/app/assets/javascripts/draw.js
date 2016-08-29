var map;
var buffer = [];
var currentRender = null;

function wktStr(i) {
    var p = buffer[i];

    return p.lng.toFixed(8).replace(",", ".") + " " + p.lat.toFixed(8).replace(",", ".");
}

function renderBuffer() {
    if (currentRender != null) {
        currentRender.setMap(null);
        currentRender = null;
    }

    var polygon = $("#polygon").is(":checked");

    if (buffer.length == 1) {
        currentRender = new google.maps.Marker({
            position: buffer[0],
            map: map
        });
    } else {
        var type = polygon ? google.maps.Polygon : google.maps.Polyline;
        currentRender = new type({
            path: buffer,
            geodesic: true,
            strokeColor: '#FF0000',
            strokeOpacity: 0.5,
            strokeWeight: 3,
            fillColor: '#FF3300',
            fillOpacity: 0.3,
            map: map
        });
    }

    // Render WKT
    var res = $("#result");
    if (buffer.length == 0) {
        res.val("");
    } else if (buffer.length == 1) {
        res.val("POINT(" + wktStr(0) + ")")
    } else {
        var str = polygon ? "POLYGON((" : "LINESTRING(";

        for (var i = 0; i < buffer.length; i++) {
            if (i != 0) {
                str += ", ";
            }

            str += wktStr(i);
        }

        if (polygon) {
            str += ", ";
            str += wktStr(0);
            str += "))";
        } else {
            str += ")";
        }

        res.val(str);
    }
}

function initMap() {
    map = new google.maps.Map(document.getElementById('mapArea'), {
        center: { lat: 55.72711009, lng: 37.53839493 },
        zoom: 13
    });

    map.addListener('click', function(e) {
        var lat = e.latLng.lat(), lng = e.latLng.lng();
        buffer.push({ lat: lat, lng: lng });
        renderBuffer();
        return false;
    });

    $("#clear").click(function () {
        buffer = [];
        renderBuffer();
    });

    $("#backspace").click(function () {
        if (buffer.length != 0) {
            buffer.pop();
            renderBuffer();
        }
    });

    $("#polygon").change(function() {
        renderBuffer();
    });
}