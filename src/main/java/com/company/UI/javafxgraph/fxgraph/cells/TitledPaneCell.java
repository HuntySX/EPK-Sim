package com.company.UI.javafxgraph.fxgraph.cells;

import com.company.UI.javafxgraph.fxgraph.graph.Cell;
import javafx.scene.control.TitledPane;

public class TitledPaneCell extends Cell {

    public TitledPaneCell(int id) {
        super(id);

        TitledPane view = new TitledPane();
        view.setPrefSize(100, 80);

        setView(view);

    }

}