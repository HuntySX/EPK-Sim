package com.company.Simulation.Simulation_Base.Data.Discrete_Data;

import com.company.EPK.Function;
import com.company.EPK.Node;
import com.company.Simulation.Simulation_Base.Data.Shared_Data.User;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Instance_Workflow {

    private Event_Instance Instance;
    private LocalTime to_Start;
    private Node node;
    private List<User> Active_User;
    private List<Resource> Active_Resource;
    private boolean Working;

    public Instance_Workflow(Event_Instance instance, LocalTime to_Start, Node node) {
        Instance = instance;
        this.to_Start = to_Start;
        this.node = node;
        this.Active_Resource = new ArrayList<>();
        this.Active_User = new ArrayList<>();
    }

    public Event_Instance getInstance() {
        return Instance;
    }

    public LocalTime getTo_Start() {
        return to_Start;
    }

    public Node getNode() {
        return node;
    }

    public void addActiveUser(User u) {
        if (!Active_User.contains(u)) {
            Active_User.add(u);
        }
    }

    public void addActiveResource(Resource res) {
        if (!Active_Resource.contains(res)) {
            Active_Resource.add(res);
        }
    }

    public void removeActiveUser(User u) {
        if (Active_User.contains(u)) {
            Active_User.remove(u);
        }
    }

    public void removeActiveResource(Resource res) {
        if (Active_Resource.contains(res)) {
            Active_Resource.remove(res);
            //TODO Stückzahl rückgeben.
        }
    }

    public boolean isWorking() {
        return Working;
    }

    public void setWorking(boolean working) {
        Working = working;
    }
}