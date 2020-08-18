package com.company.UI.EPKUI;

import com.company.EPK.Workforce;
import com.company.Enums.Option_Event_Choosing;
import com.company.Simulation.Simulation_Base.Data.Discrete_Data.*;
import com.company.Simulation.Simulation_Base.Data.Shared_Data.User;
import com.dlsc.formsfx.model.structure.*;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class UI_EXTERNAL_EVENT_MANAGER implements Initializable {
    @FXML
    Button OK_Button;
    @FXML
    VBox EDIT_Event_Button;
    @FXML
    VBox Choosebox;
    @FXML
    VBox EditResourceBox;
    @FXML
    VBox EditUserBox;

    private UI_EPK EPK;
    private Stage Mainstage;
    private Stage this_stage;
    private List<List<External_Event>> External_Events_by_Day;
    private List<User> Users;
    private List<Resource> Resources;
    private List<Workforce> Workforces;
    private SingleSelectionField<External_Event> Choose_Event;
    private SingleSelectionField<Option_Event_Choosing> Event_Choosing;
    private SingleSelectionField<Integer> Choose_Day;
    private SingleSelectionField<Integer> Show_Day;
    private IntegerField Event_Time_Second;
    private IntegerField Event_Time_Minute;
    private IntegerField Event_Time_Hour;
    private IntegerField RuntimeDays;
    private FormRenderer BooleanSettings;
    private FormRenderer Begintime;
    private FormRenderer Endingtime;
    private FormRenderer RunningDays;
    private FormRenderer Choose_UI;
    private Integer chosenDay;
    private External_Event EditableExternalEvent;
    private UI_External_Event_Manager_Status UI_Status;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        chosenDay = 0;
    }

    public void setEPK(UI_EPK EPK) {
        this.EPK = EPK;
    }

    public void setMainStage(Stage stage) {
        this.Mainstage = stage;
    }

    public void setThisstage(Stage stage) {
        this_stage = stage;
    }

    public UI_External_Event_Manager_Status getUI_Status() {
        return UI_Status;
    }

    public void setUI_Status(UI_External_Event_Manager_Status UI_Status) {
        this.UI_Status = UI_Status;
    }

    public void generateUI() {

        Choosebox.getChildren().clear();
        InstantiateStandartUI();
        generateShowUI();
        Label label = new Label("Choose User: ");
        SingleSelectionField<User> UI_USERS = Field.ofSingleSelectionType(Users).label("User");
        FormRenderer USERS_UI = new FormRenderer(Form.of(Group.of(UI_USERS)));
        Choosebox.getChildren().add(USERS_UI);
        Button button1 = new Button("Edit selected User");
        button1.setOnAction(actionEvent -> {
            User user = UI_USERS.getSelection();
            if (user != null) {
                showEditUserUI(user);
            }
        });
        Button button2 = new Button("New User");
        button2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                showNewUserUI();
            }
        });
        ButtonBar bar = new ButtonBar();
        bar.getButtons().add(button1);
        bar.getButtons().add(button2);
        Choosebox.getChildren().add(bar);
    }

    private void InstantiateStandartUI() {
        Choosebox.getChildren().clear();
        EditResourceBox.getChildren().clear();
        EditUserBox.getChildren().clear();
        Integer countday = EPK.getExternal_Events_by_Day().size() - 1;
        External_Events_by_Day = EPK.getExternal_Events_by_Day();
        Users = EPK.getAll_Users();
        Resources = EPK.getAll_Resources();

        List<External_Event_Activation_Property> User_Activation_Status = new ArrayList<>();
        User_Activation_Status.add(External_Event_Activation_Property.Activate);
        User_Activation_Status.add(External_Event_Activation_Property.Deactivate);

        SingleSelectionField UserActivation = Field.ofSingleSelectionType(User_Activation_Status);

        SingleSelectionField Userslist = Field.ofSingleSelectionType(Users).editable(false);
        IntegerField UserHour = Field.ofIntegerType(0).editable(false);
        IntegerField UserMinute = Field.ofIntegerType(0).editable(false);
        IntegerField UserSecond = Field.ofIntegerType(0).editable(false);


        List<External_Event_Activation_Property> Resource_Activation_Status = new ArrayList<>();
        Resource_Activation_Status.add(External_Event_Activation_Property.Increase);
        Resource_Activation_Status.add(External_Event_Activation_Property.Decrease);
        SingleSelectionField ResourceActivation = Field.ofSingleSelectionType(Resource_Activation_Status).editable(false);

        SingleSelectionField Resourcelist = Field.ofSingleSelectionType(Resources).editable(false);
        IntegerField ResHour = Field.ofIntegerType(0);
        IntegerField ResMinute = Field.ofIntegerType(0);
        IntegerField ResSecond = Field.ofIntegerType(0);

        ListProperty<Integer> ChooseDayProperty = new SimpleListProperty<>();
        for (int i = 0; i <= countday; i++) {
            ChooseDayProperty.add(i);
        }

        SingleSelectionField<Integer> DayField = Field.ofSingleSelectionType(ChooseDayProperty, chosenDay);
        ButtonBar ChooseDayBar = new ButtonBar();
        Button ChooseDayBtn = new Button();
        ChooseDayBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (!chosenDay.equals(DayField.getSelection())) {
                    chosenDay = DayField.getSelection();
                    InstantiateStandartUI();
                }
            }
        });
        ChooseDayBar.getButtons().add(ChooseDayBtn);
        SingleSelectionField<External_Event> Chosefield = Field.ofSingleSelectionType(External_Events_by_Day.get(chosenDay));
        Button EditChosenExternalEventBtn = new Button();
        EditChosenExternalEventBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                External_Event Chosen = Chosefield.getSelection();
                if (Chosen instanceof Resource_Activating_External_Event || Chosen instanceof Resource_Deactivating_External_Event) {
                    UI_Status = UI_External_Event_Manager_Status.EditResource;
                    EditableExternalEvent = Chosen;
                } else if (Chosen instanceof User_Activating_External_Event || Chosen instanceof User_Deactivating_External_Event) {
                    UI_Status = UI_External_Event_Manager_Status.EditUser;
                    EditableExternalEvent = Chosen;
                }
                if (Chosen != null) {
                    InstantiateStandartUI();
                }
            }
        });
        Button NewResourceEvent = new Button();
        NewResourceEvent.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                UI_Status = UI_External_Event_Manager_Status.NewResource;
                EditableExternalEvent = null;
                InstantiateStandartUI();
            }
        });
        Button NewUserEvent = new Button();
        NewUserEvent.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                UI_Status = UI_External_Event_Manager_Status.NewUser;
                EditableExternalEvent = null;
                InstantiateStandartUI();
            }
        });
        ButtonBar ActionBar = new ButtonBar();

        if (UI_Status == UI_External_Event_Manager_Status.Standard) {

        } else if (UI_Status == UI_External_Event_Manager_Status.NewUser) {

        } else if (UI_Status == UI_External_Event_Manager_Status.NewResource) {

        } else if (UI_Status == UI_External_Event_Manager_Status.EditUser) {

        } else if (UI_Status == UI_External_Event_Manager_Status.EditResource) {

        }
    }

    private void showNewUserUI() {
        Editbox.getChildren().clear();
        User user = new User("", "", EPK.getUniqueUserID(), 1);
        IntegerField P_ID = Field.ofIntegerType(user.getP_ID()).label("ID: ").editable(false);
        List<Workforce> Workforces_to_Add = new ArrayList<>();
        StringField FirstName = Field.ofStringType(user.getFirst_Name()).label("Firstname:");
        StringField LastName = Field.ofStringType(user.getLast_Name()).label("Lastname: ");
        DoubleField Efficiency = Field.ofDoubleType(user.getEfficiency()).label("Efficiency: ");

        ButtonBar Save = new ButtonBar();
        Button Save_Button = new Button("Save User");
        Save_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                user.setEfficiency(Efficiency.getValue().floatValue());
                user.setFirst_Name(FirstName.getValue());
                user.setLast_Name(LastName.getValue());
                for (Workforce w : Workforces_to_Add) {
                    if (!user.getWorkforces().contains(w)) {
                        user.getWorkforces().add(w);
                    }
                    if (!w.getGranted_to().contains(user)) {
                        w.getGranted_to().add(user);
                    }
                }
                EPK.AddUser(user);
                generateUI();
            }
        });

        Button Remove_Button = new Button("Remove user");
        Remove_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                List<Workforce> User_Workforce_List = user.getWorkforces();
                for (Workforce w : Workforces) {
                    if (w.getGranted_to().contains(user)) {
                        w.getGranted_to().remove(user);
                    }
                }
                user.getWorkforces().clear();
                EPK.getAll_Users().remove(user);
                generateUI();
            }
        });
        Remove_Button.setDisable(true);
        Save.getButtons().add(Remove_Button);
        Save.getButtons().add(Save_Button);

        Label AddedWorkforces = new Label();
        String Workforcelabel = new String("Added Workforces: [");
        for (Workforce added_force : user.getWorkforces()) {
            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
        }
        Workforcelabel = Workforcelabel.concat("]");
        AddedWorkforces.setText(Workforcelabel);

        SingleSelectionField<Workforce> Workforcelist = Field.ofSingleSelectionType(Workforces).label("Workforces: ");
        Button Add_Workforce = new Button("Add Workforce");
        Add_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Workforce force = Workforcelist.getSelection();
                if (force != null) {
                    if (!Workforces_to_Add.contains(force)) {
                        Workforces_to_Add.add(force);
                        String Workforcelabel = "Added Workforces: [";
                        for (Workforce added_force : Workforces_to_Add) {
                            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
                        }
                        Workforcelabel = Workforcelabel.concat("]");
                        AddedWorkforces.setText(Workforcelabel);
                    }
                }
            }
        });
        Button Remove_Workforce = new Button("Remove Workforce");
        Remove_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Workforce force = Workforcelist.getSelection();
                if (force != null) {
                    if (Workforces_to_Add.contains(force)) {
                        Workforces_to_Add.remove(force);
                        String Workforcelabel = new String("Added Workforces: [ ");
                        for (Workforce added_force : Workforces_to_Add) {
                            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
                        }
                        Workforcelabel = Workforcelabel.concat(" ]");
                        AddedWorkforces.setText(Workforcelabel);
                    }
                }
            }
        });
        ButtonBar WorkforcesBar = new ButtonBar();
        WorkforcesBar.getButtons().add(Add_Workforce);
        WorkforcesBar.getButtons().add(Remove_Workforce);

        FormRenderer EDIT_UI = new FormRenderer(Form.of(Group.of(P_ID, FirstName, LastName, Efficiency)));
        FormRenderer WORKFORCE_SELECTION_UI = new FormRenderer(Form.of(Group.of(Workforcelist)));
        Editbox.getChildren().add(EDIT_UI);
        Editbox.getChildren().add(AddedWorkforces);
        Editbox.getChildren().add(WORKFORCE_SELECTION_UI);
        Editbox.getChildren().add(WorkforcesBar);
        Editbox.getChildren().add(new Separator());
        Editbox.getChildren().add(Save);
    }

    private void showEditUserUI(User user) {

        Editbox.getChildren().clear();
        IntegerField P_ID = Field.ofIntegerType(user.getP_ID()).label("ID: ").editable(false);
        StringField FirstName = Field.ofStringType(user.getFirst_Name()).label("Firstname:");
        StringField LastName = Field.ofStringType(user.getLast_Name()).label("Lastname: ");
        DoubleField Efficiency = Field.ofDoubleType(user.getEfficiency()).label("Efficiency: ");
        List<Workforce> Workforces_to_Add = new ArrayList<>();
        List<Workforce> Workforces_to_Remove = new ArrayList<>();
        ButtonBar Save = new ButtonBar();
        Button Save_Button = new Button("Save User");
        Save_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                user.setEfficiency(Efficiency.getValue().floatValue());
                user.setFirst_Name(FirstName.getValue());
                user.setLast_Name(LastName.getValue());
                for (Workforce w : Workforces_to_Add) {
                    if (!user.getWorkforces().contains(w)) {
                        user.getWorkforces().add(w);
                    }
                    if (!w.getGranted_to().contains(user)) {
                        w.getGranted_to().add(user);
                    }
                }
                for (Workforce w : Workforces_to_Remove) {
                    user.getWorkforces().remove(w);
                    w.getGranted_to().remove(user);
                }
                generateUI();
            }
        });

        Button Remove_Button = new Button("Remove user");
        Remove_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                List<Workforce> User_Workforce_List = user.getWorkforces();
                for (Workforce w : Workforces) {
                    if (w.getGranted_to().contains(user)) {
                        w.getGranted_to().remove(user);
                    }
                }
                user.getWorkforces().clear();
                EPK.getAll_Users().remove(user);
                generateUI();
            }
        });
        Remove_Button.setDisable(false);
        Save.getButtons().add(Remove_Button);
        Save.getButtons().add(Save_Button);

        Label AddedWorkforces = new Label();
        String Workforcelabel = new String("Added Workforces: [");
        for (Workforce added_force : user.getWorkforces()) {
            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
        }
        Workforcelabel = Workforcelabel.concat("]");
        AddedWorkforces.setText(Workforcelabel);

        SingleSelectionField<Workforce> Workforcelist = Field.ofSingleSelectionType(Workforces).label("Workforces: ");
        Button Add_Workforce = new Button("Add Workforce");
        Add_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Workforce force = Workforcelist.getSelection();
                if (force != null) {
                    if (!user.getWorkforces().contains(force) && !Workforces_to_Add.contains(force)) {
                        Workforces_to_Add.add(force);
                        String Workforcelabel = "Added Workforces: [";
                        for (Workforce added_force : user.getWorkforces()) {
                            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
                        }
                        for (Workforce added_force : Workforces_to_Add) {
                            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
                        }
                        Workforcelabel = Workforcelabel.concat("]");
                        AddedWorkforces.setText(Workforcelabel);
                    }
                }
            }
        });

        Button Remove_Workforce = new Button("Remove Workforce");
        Remove_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                Workforce force = Workforcelist.getSelection();
                if (force != null) {
                    if (user.getWorkforces().contains(force) || Workforces_to_Add.contains(force)) {
                        Workforces_to_Remove.add(force);
                    }

                    String Workforcelabel = new String("Added Workforces: [ ");
                    for (Workforce added_force : user.getWorkforces()) {
                        if (!Workforces_to_Remove.contains(added_force)) {
                            Workforcelabel = Workforcelabel.concat("; " + added_force.toString());
                        }
                    }
                    Workforcelabel = Workforcelabel.concat(" ]");
                    AddedWorkforces.setText(Workforcelabel);
                }
            }
        });
        ButtonBar WorkforcesBar = new ButtonBar();
        WorkforcesBar.getButtons().add(Add_Workforce);
        WorkforcesBar.getButtons().add(Remove_Workforce);

        FormRenderer EDIT_UI = new FormRenderer(Form.of(Group.of(P_ID, FirstName, LastName, Efficiency)));
        FormRenderer WORKFORCE_SELECTION_UI = new FormRenderer(Form.of(Group.of(Workforcelist)));
        Editbox.getChildren().add(EDIT_UI);
        Editbox.getChildren().add(AddedWorkforces);
        Editbox.getChildren().add(WORKFORCE_SELECTION_UI);
        Editbox.getChildren().add(WorkforcesBar);
        Editbox.getChildren().add(new Separator());
        Editbox.getChildren().add(Save);
    }

    private void generateShowUI() {
        Editbox.getChildren().clear();
        StringField P_ID = Field.ofStringType("").label("ID: ").editable(false);
        StringField FirstName = Field.ofStringType((String) "").label("Firstname: ");
        StringField LastName = Field.ofStringType((String) "").label("Lastname: ");
        StringField Efficiency = Field.ofStringType("").label("Efficiency: ");

        ButtonBar Save = new ButtonBar();
        Button Save_Button = new Button("Save User");
        Save_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
            }
        });

        Button Remove_Button = new Button("Remove user");
        Remove_Button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
            }
        });
        Remove_Button.setDisable(true);
        Save.getButtons().add(Remove_Button);
        Save.getButtons().add(Save_Button);
        List<Workforce> emptyList = new ArrayList<>();
        Label AddedWorkforces = new Label(" ");
        SingleSelectionField<Workforce> Workforcelist = Field.ofSingleSelectionType(emptyList).label("Workforces: ");
        Button Add_Workforce = new Button("Add Workforce");
        Add_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
            }
        });

        Button Remove_Workforce = new Button("Remove Workforce");
        Remove_Workforce.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
            }
        });

        ButtonBar WorkforcesBar = new ButtonBar();
        WorkforcesBar.getButtons().add(Add_Workforce);
        WorkforcesBar.getButtons().add(Remove_Workforce);

        P_ID.editable(false);
        FirstName.editable(false);
        LastName.editable(false);
        Efficiency.editable(false);
        Workforcelist.editable(true);
        Add_Workforce.setDisable(true);
        Remove_Workforce.setDisable(true);
        Save_Button.setDisable(true);
        FormRenderer EDIT_UI = new FormRenderer(Form.of(Group.of(P_ID, FirstName, LastName, Efficiency)));
        FormRenderer WORKFORCE_SELECTION_UI = new FormRenderer(Form.of(Group.of(Workforcelist)));
        Editbox.getChildren().add(EDIT_UI);
        Editbox.getChildren().add(AddedWorkforces);
        Editbox.getChildren().add(WORKFORCE_SELECTION_UI);
        Editbox.getChildren().add(WorkforcesBar);
        Editbox.getChildren().add(new Separator());
        Editbox.getChildren().add(Save);
    }


    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == OK_Button) {
            this_stage.close();
        }
    }

    public void setExternal_Events_by_Day(List<List<External_Event>> external_Events_by_Day) {
        External_Events_by_Day = external_Events_by_Day;
    }
}

