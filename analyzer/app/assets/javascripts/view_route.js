function initMap() {
    var avgLat = 0, avgLng = 0;

    $.each(route.forward, function(t) {
        avgLat += this.lat;
        avgLng += this.lng;
    });

    avgLat /= route.forward.length;
    avgLng /= route.forward.length;

    map = new google.maps.Map(document.getElementById('mapArea'), {
        center: { lat: avgLat, lng: avgLng },
        zoom: 10
    });

    new google.maps.Polyline({
        path: route.forward,
        geodesic: true,
        strokeColor: '#FF0000',
        strokeOpacity: 0.5,
        strokeWeight: 3,
        map: map
    });

    new google.maps.Polyline({
        path: route.backward,
        geodesic: true,
        strokeColor: '#0000FF',
        strokeOpacity: 0.5,
        strokeWeight: 3,
        map: map
    });

    $.each(route.stops, function(i) {
        new google.maps.Marker({
            position: this,
            label: (i + 1).toString(),
            title: this.name,
            map: map
        });
    })
}