
package src.view.Attendance; // Or a suitable ui.controls package if moved out

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// This class can be a public static inner class of ClassroomAttendanceView
// or a separate public class in its own file.
public class StarRatingControl extends HBox {
    private IntegerProperty rating = new SimpleIntegerProperty(0);
    private int maxStars = 5;
    private List<Label> starsList;
    private static final String FILLED_STAR = "★";
    private static final String EMPTY_STAR = "☆";
    private static final String STAR_COLOR_FILLED = "-fx-text-fill: #ffc107;"; // Gold color for filled stars
    private static final String STAR_COLOR_EMPTY = "-fx-text-fill: #cccccc;";  // Light gray for empty stars

    public StarRatingControl(int initialRating, int maxStars, boolean editable) {
        this.maxStars = maxStars;
        this.rating.set(initialRating);
        this.setSpacing(2);
        this.setAlignment(Pos.CENTER);

        starsList = IntStream.range(0, this.maxStars)
                .mapToObj(i -> {
                    Label starLabel = new Label();
                    starLabel.setFont(Font.font(18)); // Adjust size as needed
                    if (editable) {
                        starLabel.setOnMouseClicked(event -> setRating(i + 1));
                    }
                    return starLabel;
                })
                .collect(Collectors.toList());
        getChildren().addAll(starsList);
        updateStarsVisual();
        rating.addListener((obs, oldVal, newVal) -> updateStarsVisual());
    }

    public StarRatingControl(IntegerProperty ratingProperty, int maxStars, boolean editable) {
        this(ratingProperty.get(), maxStars, editable); // Call the main constructor
        this.rating.bindBidirectional(ratingProperty); // Bind to the external property
    }

    private void updateStarsVisual() {
        for (int i = 0; i < maxStars; i++) {
            if (i < rating.get()) {
                starsList.get(i).setText(FILLED_STAR);
                starsList.get(i).setStyle(STAR_COLOR_FILLED);
            } else {
                starsList.get(i).setText(EMPTY_STAR);
                starsList.get(i).setStyle(STAR_COLOR_EMPTY);
            }
        }
    }

    public int getRating() { return rating.get(); }
    public void setRating(int rating) {
        this.rating.set(Math.max(0, Math.min(rating, maxStars)));
    }
    public IntegerProperty ratingProperty() { return rating; }
}

