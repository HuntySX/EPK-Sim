package com.company.UI.javafxgraph.fxgraph.cells;

import com.company.UI.javafxgraph.fxgraph.graph.Cell;
import javafx.scene.image.ImageView;

public class ImageCell extends Cell {

    public ImageCell(int id) {
        super(id);

        ImageView view = new ImageView("http://upload.wikimedia.org/wikipedia/commons/thumb/4/41/Siberischer_tiger_de_edit02.jpg/800px-Siberischer_tiger_de_edit02.jpg");
        view.setFitWidth(100);
        view.setFitHeight(80);

        setView(view);

    }

}