package com.vaadin.example.sightseeing.views.map;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.example.sightseeing.data.Role;
import com.vaadin.example.sightseeing.data.entity.Place;
import com.vaadin.example.sightseeing.data.generator.DataGenerator;
import com.vaadin.example.sightseeing.data.service.PlaceRepository;
import com.vaadin.example.sightseeing.security.AuthenticatedUser;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.map.Map;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.map.configuration.View;
import com.vaadin.flow.component.map.configuration.feature.LineStringFeature;
import com.vaadin.flow.component.map.configuration.feature.MarkerFeature;
import com.vaadin.flow.component.map.configuration.style.Stroke;
import com.vaadin.flow.component.map.configuration.style.TextStyle;
import com.vaadin.flow.component.map.events.MapClickEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.PermitAll;

@PageTitle("Map")
@Route(value = "map")
@RouteAlias(value = "")
@StyleSheet("map-view.css")
@PermitAll
public class MapView extends VerticalLayout {

    @Autowired
    private PlaceRepository service;

    private Map map = new Map();

    public MapView(AuthenticatedUser authenticatedUser) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        authenticatedUser.get().ifPresent(u -> {
            if (u.getRoles().contains(Role.ADMIN)) {
                add(new AdminNav("map"));
            }
        });

        Dialog dialog = new Dialog();
        add(dialog);

        map.getElement().setAttribute("theme", "borderless");
        map.addClickListener(getMapClickListener(dialog));
        View view = map.getView();
        view.setCenter(DataGenerator.CENTER);
        view.setZoom(12);

        LineStringFeature oldOffice = outline(DataGenerator.CENTER, 0.002, 0.001);
        map.getFeatureLayer().addFeature(oldOffice);

        LineStringFeature newOffice = outline(
                new Coordinate(22.29158560493023, 60.44957041468254),
                0.001,
                0.0005);
        newOffice.getStyle().setStroke(new Stroke("purple", 4));
        map.getFeatureLayer().addFeature(newOffice);

        addAndExpand(map);
    }

    private LineStringFeature outline(Coordinate center, double deltaX,
            double deltaY) {
        LineStringFeature route = new LineStringFeature(
                new Coordinate(center.getX() + deltaX, center.getY() + deltaY),
                new Coordinate(center.getX() + deltaX, center.getY() - deltaY),
                new Coordinate(center.getX() - deltaX, center.getY() - deltaY),
                new Coordinate(center.getX() - deltaX, center.getY() + deltaY),
                new Coordinate(center.getX() + deltaX, center.getY() + deltaY));
        return route;
    }

    private ComponentEventListener<MapClickEvent> getMapClickListener(
            Dialog dialog) {
        return e -> {
            dialog.removeAll();
            Coordinate coordinate = e.getCoordinate();
            dialog.setHeaderTitle(coordinate.getY() + ", " + coordinate.getX());
            Double specificity = 0.001;
            List<Place> places = getNearby(coordinate, specificity);
            while (places.isEmpty() || places.size() < 3) {
                specificity += 0.001;
                places = getNearby(coordinate, specificity);
                if (specificity >= 0.01) {
                    break;
                }
            }
            if (!places.isEmpty()) {
                Collections.sort(places, getPlaceComparator(coordinate));
            }
            int count = places.size();
            StringBuilder sb = new StringBuilder("<span>Nearby: " + count);
            int index = 0;
            for (Place place : places) {
                if (index >= 10) {
                    sb.append("...");
                    break;
                }
                sb.append("<br>");
                sb.append(place.getName());
                sb.append(" (");
                sb.append(Math.round(getDistance(coordinate, place)));
                sb.append(" m)");
                ++index;
            }
            sb.append("</span>");
            dialog.add(new Html(sb.toString()));
            dialog.add(new Html("<br>"));
            Button button = new Button("Add marker!", event -> {
                // Create a marker with a custom text style
                TextStyle textStyle = new TextStyle();
                // Customize font and color
                textStyle.setFont("bold 16px sans-serif");
                textStyle.setStroke("#fdf4ff", 3);
                textStyle.setFill("#701a75");
                // Position text to the right of the icon
                textStyle.setTextAlign(TextStyle.TextAlign.LEFT);
                textStyle.setOffset(22, -18);

                MarkerFeature customizedMarker = new MarkerFeature(coordinate);
                customizedMarker.setText("Nearby: " + count);
                customizedMarker.setTextStyle(textStyle);
                map.getFeatureLayer().addFeature(customizedMarker);
            });
            button.addClassName("myButton");
            dialog.add(button);
            dialog.open();
        };
    }

    private List<Place> getNearby(Coordinate coordinate, Double specificity) {
        List<Place> list = service.findByXBetweenAndYBetween(
                coordinate.getX() - specificity,
                coordinate.getX() + specificity,
                coordinate.getY() - specificity,
                coordinate.getY() + specificity);
        return list;
    }

    private Comparator<Place> getPlaceComparator(Coordinate coordinate) {
        return new Comparator<Place>() {
            @Override
            public int compare(Place p1, Place p2) {
                double distance1 = getDistance(coordinate, p1);
                double distance2 = getDistance(coordinate, p2);
                return Double.compare(distance1, distance2);
            }
        };
    }

    private double getDistance(Coordinate coordinate, Place p1) {
        return distance(coordinate.getY(), p1.getY(), coordinate.getX(),
                p1.getX(), 0, 0);
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * https://stackoverflow.com/a/16794680/15861571
     *
     * @returns Distance in Meters
     *
     */
    public static double distance(double lat1, double lat2, double lon1,
            double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
}
