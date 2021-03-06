package com.company.UI.EPKUI;

import com.company.EPK.EPK_Node;
import com.company.EPK.Event_Con_Join;
import com.company.Enums.Contype;
import com.dlsc.formsfx.model.structure.*;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class UI_XOR_Join extends Event_Con_Join implements UI_Instantiable {

    private VBox Box;
    private VBox Rightbox;
    private List<EPK_Node> nodelist;
    private List<EPK_Node> prev_nodelist;
    private UI_EPK EPK;
    private IntegerField UI_ID_FIELD;
    private IntegerProperty UI_ID;
    private SingleSelectionField<EPK_Node> UI_NEXT_ELEMENTS_FIELD;
    private SingleSelectionField<EPK_Node> UI_PREVIOUS_ELEMS;
    private final BooleanProperty IS_EAGER = new SimpleBooleanProperty(true);
    private BooleanField UI_IS_EAGER;
    private final Label SelectionLabel = new Label();
    private FormRenderer ID_TAG_UI;
    private FormRenderer NEXT_ELEMS_UI;
    private FormRenderer IS_EAGER_UI;
    private List<EPK_Node> Chosen_Previous_List;


    public UI_XOR_Join(int ID, UI_EPK EPK, VBox Rightbox) {
        super(null, ID, Contype.EAGER_XOR);
        this.Box = new VBox();
        this.Rightbox = Rightbox;
        this.EPK = EPK;
        this.UI_ID = new SimpleIntegerProperty(ID);
        this.prev_nodelist = new ArrayList<>();
        this.Chosen_Previous_List = getPrevious_Elements();
        UI_ID_FIELD = Field.ofIntegerType(UI_ID).label("ID").editable(false);
        this.nodelist = getNext_Elem();

        ID_TAG_UI = new FormRenderer(
                Form.of(
                        Group.of(
                                UI_ID_FIELD)));
        UI_IS_EAGER = Field.ofBooleanType(IS_EAGER).label("XOR-Typ:").tooltip("Wenn das H??ckchen gesetzt ist," +
                "arbeitet der XOR-Join Eager (Alle vorg??nger werden auf potentielles Abarbeiten" +
                "??berpr??ft. Ansonsten arbeitet der XOR-Join Lazy(Sobald eine Instanz ankommt wird" +
                "auf die g??ltige Or Bedingung gepr??ft)").editable(true);
        IS_EAGER_UI = new FormRenderer(Form.of(Group.of(UI_IS_EAGER)));
    }

    public UI_XOR_Join(List<EPK_Node> Next_Elem, int ID, Contype contype) {
        super(Next_Elem, ID, contype);
    }

    @Override
    public List<EPK_Node> getNodelist() {
        return nodelist;
    }

    public VBox Get_UI() {
        prev_nodelist = EPK.getAll_Elems();
        Box.getChildren().clear();
        Box.getChildren().add(ID_TAG_UI);
        Box.getChildren().add(new Separator());
        UI_NEXT_ELEMENTS_FIELD = Field.ofSingleSelectionType(nodelist).label("Nachfolger");
        NEXT_ELEMS_UI = new FormRenderer(Form.of(Group.of(UI_NEXT_ELEMENTS_FIELD)));
        Box.getChildren().add(NEXT_ELEMS_UI);
        Box.getChildren().add(new Separator());
        Button btn = new Button("Verbindung entfernen");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                EPK_Node Node = UI_NEXT_ELEMENTS_FIELD.getSelection();
                if (Node != null) {
                    EPK.getActive_Elem().getEPKNode().getNext_Elem().remove(Node);
                    EPK.getModel().removeEdge(getID(), Node.getID());
                    EPK.getGraph().endUpdate();
                    EPK.activate();
                }
            }
        });
        Box.getChildren().add(btn);
        ButtonBar Next_ElemBar = new ButtonBar();
        Next_ElemBar.getButtons().add(btn);
        Box.getChildren().add(Next_ElemBar);
        Button Save_Selection = new Button("Add previous");
        Save_Selection.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                EPK_Node Chosen_Previous = UI_PREVIOUS_ELEMS.getSelection();
                if (Chosen_Previous != null && !Chosen_Previous_List.contains(Chosen_Previous)) {

                    Chosen_Previous_List.add(Chosen_Previous);

                }
                StringBuilder LabelString = new StringBuilder();
                for (EPK_Node node : Chosen_Previous_List) {
                    LabelString.append(node.toString());
                    LabelString.append("\n");
                }
                if (!LabelString.equals("")) {
                    SelectionLabel.setText(LabelString.toString());
                }
            }
        });
        Button Delete_Selection = new Button("Delete previous");
        Delete_Selection.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                EPK_Node Chosen_to_Delete = UI_PREVIOUS_ELEMS.getSelection();
                if (Chosen_to_Delete != null) {
                    Chosen_Previous_List.remove(Chosen_to_Delete);
                    StringBuilder LabelString = new StringBuilder();
                    for (EPK_Node node : Chosen_Previous_List) {
                        LabelString.append(node.toString());
                        LabelString.append("\n");
                    }
                    if (!LabelString.equals("")) {
                        SelectionLabel.setText(LabelString.toString());
                    }
                }
            }
        });
        UI_PREVIOUS_ELEMS = Field.ofSingleSelectionType(prev_nodelist).label("Vorg??nger: ")
                .tooltip("Vorg??nger werden in der ??berpr??fung der Instanz zur weitergabe am Gate auf" +
                        "Beendigung gepr??ft (je nach Gate Regel)");
        FormRenderer PREVIOUS_ELEMS_UI = new FormRenderer(Form.of
                (Group.of
                        (UI_PREVIOUS_ELEMS)));


        Box.getChildren().add(new Separator());
        Box.getChildren().add(IS_EAGER_UI);
        Box.getChildren().add(new Separator());
        Box.getChildren().add(SelectionLabel);
        Box.getChildren().add(PREVIOUS_ELEMS_UI);
        ButtonBar Bar = new ButtonBar();
        Bar.getButtons().add(Save_Selection);
        Bar.getButtons().add(Delete_Selection);
        Box.getChildren().add(Bar);


        return Box;
    }

    @Override
    public void save_Settings() {
        if (UI_IS_EAGER.getValue()) {
            setContype(Contype.EAGER_XOR);
        } else {
            setContype(Contype.LAZY_XOR);
        }
    }

    @Override
    public int get_Next_Elem_ID() {
        return 0;
    }

    @Override
    public String toString() {
        return "XOR-Join [" +
                "ID: " + UI_ID_FIELD.getValue() + "]";
    }

    @Override
    public EPK_Node getthis() {
        return super.returnUpperClass();
    }
}
